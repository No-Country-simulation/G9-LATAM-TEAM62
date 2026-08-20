"""
Procesador de cartolas bancarias -- interfaz de línea de comandos.

Misma lógica que analisis_cartola.ipynb, extraída a script invocable.
Uso:
    python3 procesar_cartola_cli.py <ruta_al_archivo> [--anio-defecto 2026] [--pais CL]

Salida: un único JSON por stdout (ver docs/contrato-ingesta-cartolas.md).
Código de salida: 0 si status=ok, 1 si status=error.
"""

import re
import unicodedata
from collections import Counter
from pathlib import Path

import numpy as np
import pandas as pd

try:
    import holidays as _holidays
except Exception:            # el pipeline sigue funcionando sin la librería
    _holidays = None

# Cada concepto canónico -> subcadenas que pueden aparecer en el encabezado real.
# El orden importa: se evalúan de más específico a más genérico dentro del match.
ALIAS_ENCABEZADO = {
    "fecha":       ["FECHA", "DATE"],
    "descripcion": ["DESCRIPCION", "DESCRIPTION", "DETALLE", "GLOSA", "CONCEPTO",
                    "CONCEPT", "TRANSACTION_TYPE", "TRANSACTION TYPE",
                    "TIPO DE TRANSACCION", "TYPE"],
    "cargo":       ["CARGO", "CHEQUE", "GIRO", "DEBITO", "DEBIT"],
    "abono":       ["ABONO", "DEPOSITO", "CREDITO", "CREDIT", "HABER"],
    "monto":       ["MONTO DE TRANSACCION", "NET_AMOUNT", "NET AMOUNT", "AMOUNT",
                    "MONTO", "IMPORTE", "VALOR"],
    "saldo":       ["SALDO", "BALANCE"],
}

NOMBRE_CANONICO = {
    "fecha": "FECHA", "descripcion": "DESCRIPCION", "cargo": "CARGO",
    "abono": "ABONO", "monto": "MONTO", "saldo": "SALDO",
}

# Meses en español (abreviados y completos) -> número.
MESES_ES = {
    "ENERO": "01", "ENE": "01", "FEBRERO": "02", "FEB": "02",
    "MARZO": "03", "MAR": "03", "ABRIL": "04", "ABR": "04",
    "MAYO": "05", "MAY": "05", "JUNIO": "06", "JUN": "06",
    "JULIO": "07", "JUL": "07", "AGOSTO": "08", "AGO": "08",
    "SEPTIEMBRE": "09", "SEP": "09", "SET": "09", "OCTUBRE": "10", "OCT": "10",
    "NOVIEMBRE": "11", "NOV": "11", "DICIEMBRE": "12", "DIC": "12",
}

# Señales de país en el texto de la cartola - código ISO para `holidays`.
SENALES_PAIS = {
    "CL": ["CHILE", "SANTIAGO", "CLP", "BANCOESTADO", "CUENTARUT", "R.U.T", "RUT",
           "BANCO FALABELLA", "BANCO SANTANDER", "CMF"],
    "AR": ["ARGENTINA", "BUENOS AIRES", "ARS", "CUIT", "AFIP", "BCRA"],
    "PE": ["PERU", "PERÚ", "LIMA", "PEN", "SUNAT", "SOLES"],
    "MX": ["MEXICO", "MÉXICO", "MXN", "CDMX", "RFC", "SAT"],
    "CO": ["COLOMBIA", "BOGOTA", "BOGOTÁ", "COP", "DIAN"],
}

# ---------------------------------------------------------------------------
# Categorías alineadas con Category.java del backend real (13 categorías).
# No se agrega ninguna categoría nueva -- nos adaptamos a lo que backend ya
# tiene, no al revés. Separadas en gasto/ingreso porque backend también las
# separa por TransactionType (EXPENSE vs INCOME vs SAVING).
# ---------------------------------------------------------------------------
CATEGORIAS_GASTO = {
    "FOOD": ["JUMBO", "SUSHI", "LULO", "NINA", "BURGER", "KFC", "SABORES", "CUGAT",
             "LIDER", "FUDO", "MINIMARKET", "ACUENTA", "ALMACEN", "CAFETERIA",
             "RESTAURANT", "CARNES", "DON XIN", "AHUM"],
    "TRANSPORT": ["SHELL", "ARAMCO", "COPEC", "UBER", "RED MOVILIDAD", "METRO",
                  "PETROBRAS", "TERPEL"],
    "HEALTH": ["CRUZ VERDE", "CLINICA", "VETER", "FARMACIA", "SALCOBRAND", "AHUMADA"],
    "ENTERTAINMENT": ["SPOTIFY", "YOUTUBE", "APPLE", "PLAZA MAULE", "MUVIX",
                      "NETFLIX", "DISNEY", "STEAM", "CLAUDE"],
    # antes "SERVICES": se renombra a UTILITIES (nombre real del backend) y se
    # suman las utilities reales que faltaban (ENEL, AGUAS ANDINAS, VTR, GASCO).
    "UTILITIES": ["PAYU", "MERCADOPAGO", "WEBPAY", "MULTIPAGO", "EPARIS", "TOKU",
                  "ENTEL", "MOVISTAR", "WOM", "CLARO", "EQUIFAX",
                  "ENEL", "AGUAS ANDINAS", "VTR", "GASCO"],
    # categorías nuevas para cubrir las 13 reales del backend (antes no existían)
    "HOUSING": ["GASTO COMUN", "GC ", "ARRIENDO", "DIVIDENDO", "PORTALINMOBILIARIO"],
    "EDUCATION": ["COLEGIO", "UNIVERSIDAD", "DUOC", "INACAP", "UDEMY", "COURSERA"],
    "SHOPPING": ["FALABELLA", "PARIS", "RIPLEY", "ZARA", "DAFITI", "H&M"],
}

CATEGORIAS_INGRESO = {
    "SALARY": ["REMUNERACION", "SUELDO", "LIQUIDACION SUELDO", "HONORARIOS"],
}

# Transferencias entre cuentas (TEF, giros): siguen registrándose como
# OTHER_EXPENSE/OTHER_INCOME según dirección -- backend no tiene una
# categoría de "transferencia" propia y no la agregamos nosotros. Esta lista
# se usa aparte, a la hora de analizar presupuesto, para excluirlas del
# cálculo de % de gasto (ver budget_recommendation_engine.py: is_transfer()).
TRANSFER_KEYWORDS = ["TRANSF", "TEF", "TRANSFERENCIA", "GIRO"]


def _sin_acentos(texto: str) -> str:
    texto = unicodedata.normalize("NFKD", str(texto))
    return "".join(c for c in texto if not unicodedata.combining(c))


def normalizar_encabezado(texto) -> str:
    return _sin_acentos(str(texto)).upper().strip()


def limpiar_numero(x):
    """Convierte cualquier representación de monto a float, o NaN si no aplica.
    Maneja $ separador de miles y decimales"""
    if x is None or (isinstance(x, float) and np.isnan(x)):
        return np.nan
    if isinstance(x, (int, float, np.integer, np.floating)):
        return float(x)
    s = str(x).strip()
    negativo = s.startswith("(") and s.endswith(")")  # (1.234) negativo contable
    s = re.sub(r"[^0-9,.\-]", "", s)                    # quita $, espacios, letras, porcentajes
    if s in ("", "-", ".", ","):
        return np.nan
    tiene_coma, tiene_punto = "," in s, "." in s
    if tiene_coma and tiene_punto:
        # El separador que aparece más a la derecha es el decimal
        if s.rfind(",") > s.rfind("."):
            s = s.replace(".", "").replace(",", ".")
        else:
            s = s.replace(",", "")
    elif tiene_coma:
        # Solo coma, decimal si va seguida de 2 dígitos al final
        s = s.replace(",", ".") if re.search(r",\d{2}$", s) else s.replace(",", "")
    elif tiene_punto:
        # Solo punto: si todos los grupos tras el primero son de 3 dígitos entonces son miles.
        grupos = s.replace("-", "").split(".")
        if len(grupos) > 1 and all(len(g) == 3 for g in grupos[1:]):
            s = s.replace(".", "")
    try:
        val = float(s)
    except ValueError:
        return np.nan
    return -val if negativo and val > 0 else val

def _preparar_fecha_str(valor, anio_defecto: int) -> str:
    s = _sin_acentos(str(valor)).upper().strip()
    if not s or s in ("NAN", "NAT", "NONE"):
        return ""
    # Reemplaza nombres de mes en español por su número.
    for nombre, num in MESES_ES.items():
        s = re.sub(rf"\b{nombre}\b", num, s)
    s = s.replace("-", "/").replace(".", "/") #unificar separadores de fecha
    s = re.sub(r"\s+", " ", s).strip()
    # si no trae separador de año, se lo añadimos al bloque dd/mm.
    if not re.search(r"\d{4}", s):
        m = re.match(r"^(\d{1,2}/\d{1,2})(.*)$", s)
        if m:
            s = f"{m.group(1)}/{anio_defecto}{m.group(2)}"
    return s


def parsear_fechas(serie: pd.Series, anio_defecto: int) -> pd.Series:
    prep = serie.map(lambda v: _preparar_fecha_str(v, anio_defecto))
    # `format="mixed"` infiere el formato por elemento: soporta dd/mm/yyyy,
    # dd/mm/yyyy HH:MM:SS, etc. en una misma columna sin romperse.
    return pd.to_datetime(prep, dayfirst=True, errors="coerce", format="mixed")

def _conceptos_en_fila(fila, alias=ALIAS_ENCABEZADO) -> set:
    celdas = [normalizar_encabezado(c) for c in fila]
    return {concepto for concepto, subs in alias.items()
            if any(a in celda for celda in celdas for a in subs)}


def _contar_conceptos(fila, alias=ALIAS_ENCABEZADO) -> int:
    return len(_conceptos_en_fila(fila, alias))


def detectar_fila_encabezado(raw: pd.DataFrame, minimo: int = 3):
    """Devuelve el índice de la fila de encabezado, o None si ninguna alcanza `minimo`.
    Da peso extra a la presencia de `fecha`: toda tabla de movimientos tiene columna
    de fecha, así el encabezado real gana a filas-resumen (p. ej. saldos/totales)."""
    if raw.empty:
        return None

    def puntaje(fila):
        conceptos = _conceptos_en_fila(fila)
        return len(conceptos) + (2 if "fecha" in conceptos else 0)

    puntajes = raw.apply(puntaje, axis=1)
    fila = int(puntajes.idxmax())
    # el mínimo se valida sobre el número REAL de conceptos, sin el bono de fecha
    if _contar_conceptos(raw.iloc[fila]) < minimo:
        return None
    return fila


def detectar_pais(raw: pd.DataFrame, defecto: str = "CL") -> str:
    texto = " ".join(normalizar_encabezado(c) for c in raw.head(30).to_numpy().ravel()
                     if not (isinstance(c, float) and np.isnan(c)))
    puntajes = {pais: sum(s in texto for s in senales) for pais, senales in SENALES_PAIS.items()}
    mejor = max(puntajes, key=puntajes.get)
    return mejor if puntajes[mejor] > 0 else defecto


def inferir_anio(raw: pd.DataFrame, ruta: str | None = None) -> int:
    """Busca un año de 4 dígitos en el nombre del archivo o en la cabecera; si no, usa el actual."""
    if ruta:
        m = re.search(r"(20\d{2})", Path(ruta).name)
        if m:
            return int(m.group(1))
    texto = " ".join(str(c) for c in raw.head(30).to_numpy().ravel())
    m = re.search(r"\b(20\d{2})\b", texto)
    return int(m.group(1)) if m else pd.Timestamp.now().year

def _leer_csv(ruta: str) -> pd.DataFrame:
    for enc in ("utf-8-sig", "latin-1"):
        try:
            return pd.read_csv(ruta, header=None, sep=None, engine="python",
                               encoding=enc, dtype=str, on_bad_lines="skip")
        except (UnicodeDecodeError, pd.errors.ParserError):
            continue
    return pd.read_csv(ruta, header=None, sep=";", encoding="latin-1",
                       dtype=str, on_bad_lines="skip")


FORMATOS_SOPORTADOS = (".xlsx", ".xls", ".xlsm", ".csv", ".pdf")


def _bordes_desde_tablas(page):
    """Bordes de columna (coords x) tomados de la tabla RAYADA cuyo encabezado
    concentra más conceptos y contiene fecha. None si no hay tabla utilizable."""
    mejor, mejor_score = None, 0
    for t in page.find_tables():
        try:
            filas = t.extract()
        except Exception:
            continue
        for fila in filas:
            conceptos = _conceptos_en_fila(fila)
            if "fecha" in conceptos and len(conceptos) > mejor_score:
                mejor, mejor_score = t, len(conceptos)
    if mejor is None or mejor_score < 3:
        return None
    bordes = set()
    for fila in mejor.rows:
        for celda in fila.cells:
            if celda:
                bordes.add(round(celda[0], 1))
                bordes.add(round(celda[2], 1))
    return sorted(bordes)


def _bordes_por_gap(palabras, factor_gap=2.5):
    """Fallback para PDF sin líneas: bordes por separación entre palabras,
    con umbral adaptativo al tamaño de fuente."""
    altura = float(np.median([w["bottom"] - w["top"] for w in palabras])) or 1.0
    gap = factor_gap * altura
    xs = sorted(w["x0"] for w in palabras)
    bordes = [xs[0]]
    for a, b in zip(xs, xs[1:]):
        if b - a > gap:
            bordes.append(b)
    bordes.append(max(w["x1"] for w in palabras) + 1)
    return bordes


def _grid_de_palabras(palabras, bordes, tol_linea=3.0):
    ncols = len(bordes) - 1

    def columna(x):
        ci = 0
        for i in range(ncols):
            if x >= bordes[i]:
                ci = i
        return ci

    lineas: dict[int, list] = {}
    for w in palabras:
        lineas.setdefault(round(w["top"] / tol_linea), []).append(w)

    filas = []
    for clave in sorted(lineas):
        celdas = [""] * ncols
        for w in sorted(lineas[clave], key=lambda w: w["x0"]):
            ci = columna(w["x0"])
            celdas[ci] = (celdas[ci] + " " + w["text"]).strip()
        filas.append(celdas)
    return filas


def _leer_pdf(ruta: str, tol_linea: float = 3.0) -> pd.DataFrame:
    """Reconstruye la(s) tabla(s) de un PDF de texto. No requiere Java.
    - Preferido: usa las líneas verticales reales de la tabla como bordes de
      columna (alineación exacta) y el texto para las filas -> captura todas.
    - Fallback (PDF sin líneas): agrupa por posición de palabras con umbral
      adaptativo al tamaño de fuente.
    PDF escaneado (sin texto) -> lanza y el orquestador reporta."""
    try:
        import pdfplumber
    except ImportError as e:
        raise RuntimeError("Falta pdfplumber para leer PDF (pip install pdfplumber)") from e

    partes = []
    with pdfplumber.open(ruta) as pdf:
        for page in pdf.pages:
            palabras = page.extract_words()
            if not palabras:
                continue
            bordes = _bordes_desde_tablas(page)
            tabla = None
            if bordes and len(bordes) >= 3:
                tabla = page.extract_table({
                    "vertical_strategy": "explicit",
                    "explicit_vertical_lines": bordes,
                    "horizontal_strategy": "text",
                    "snap_tolerance": 4,
                })
            if tabla:
                partes.append(pd.DataFrame(tabla))
            else:
                gap = _bordes_por_gap(palabras)
                partes.append(pd.DataFrame(_grid_de_palabras(palabras, gap, tol_linea)))

    if not partes:
        raise ValueError("PDF sin texto extraíble (¿escaneado? requeriría OCR).")
    ancho = max(p.shape[1] for p in partes)
    partes = [p.reindex(columns=range(ancho)) for p in partes]
    return pd.concat(partes, ignore_index=True)


def leer_archivo(ruta: str) -> pd.DataFrame:
# Lee el archivo sin encabezado
    ext = Path(ruta).suffix.lower()
    if ext in (".xlsx", ".xls", ".xlsm"):
        return pd.read_excel(ruta, header=None)
    if ext == ".csv":
        return _leer_csv(ruta)
    if ext == ".pdf":
        return _leer_pdf(ruta)
    raise ValueError(f"Formato no soportado: {ext}")

def extraer_tabla(raw: pd.DataFrame, fila_encabezado: int) -> pd.DataFrame:
    encabezados = raw.iloc[fila_encabezado].tolist()
    cuerpo = raw.iloc[fila_encabezado + 1:].reset_index(drop=True).copy()
    cuerpo.columns = range(cuerpo.shape[1])

    col_por_concepto: dict[str, int] = {}
    for idx, enc in enumerate(encabezados):
        enc_norm = normalizar_encabezado(enc)
        if not enc_norm:
            continue
        for concepto, subs in ALIAS_ENCABEZADO.items():
            if concepto not in col_por_concepto and any(a in enc_norm for a in subs):
                col_por_concepto[concepto] = idx
                break

    salida = pd.DataFrame(index=cuerpo.index)
    for concepto, idx in col_por_concepto.items():
        salida[NOMBRE_CANONICO[concepto]] = cuerpo[idx]
    return salida


def normalizar_montos(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    for col in ("CARGO", "ABONO", "MONTO", "SALDO"):
        if col in df.columns:
            df[col] = df[col].map(limpiar_numero)

    # Construye `monto` con signo a partir de lo disponible.
    if "ABONO" in df.columns or "CARGO" in df.columns:
        abono = df["ABONO"] if "ABONO" in df.columns else 0.0
        cargo = df["CARGO"] if "CARGO" in df.columns else 0.0
        df["monto"] = pd.Series(abono, index=df.index).fillna(0) - \
                      pd.Series(cargo, index=df.index).fillna(0)
    elif "MONTO" in df.columns:
        df["monto"] = df["MONTO"].fillna(0)
    else:
        df["monto"] = 0.0

    df["tipo_movimiento"] = np.where(df["monto"] > 0, "abono",
                             np.where(df["monto"] < 0, "cargo", "neutro"))
    df["monto_abs"] = df["monto"].abs()
    return df


def limpiar_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    if "DESCRIPCION" not in df.columns:
        df["DESCRIPCION"] = ""
    df = df.dropna(subset=["FECHA"]) if "FECHA" in df.columns else df
    df = df[df["DESCRIPCION"].notna()]
    df = df.copy()
    df["DESCRIPCION"] = df["DESCRIPCION"].astype(str).str.strip()
    df = df[df["DESCRIPCION"] != ""]
    return df.reset_index(drop=True)

def limpiar_texto(desc: str) -> str:
    desc = _sin_acentos(str(desc)).upper()
    desc = re.sub(r"\d{4,}", "", desc)
    return re.sub(r"[^A-ZÑ\s]", " ", desc)


def agregar_descripcion_limpia(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    df["desc_limpia"] = df["DESCRIPCION"].map(limpiar_texto)
    return df


def categorizar(descripcion: str, tipo_movimiento: str,
                 categorias_gasto=CATEGORIAS_GASTO, categorias_ingreso=CATEGORIAS_INGRESO) -> str:
    """Devuelve SIEMPRE una categoría válida del Category.java real del backend.
    La dirección del movimiento (abono/cargo) decide qué diccionario de
    palabras clave se evalúa, y también el fallback cuando ninguna regla
    coincide -- así nunca se etiqueta un ingreso como categoría de gasto
    (o viceversa), algo que la versión anterior no garantizaba."""
    desc = _sin_acentos(str(descripcion)).upper()
    candidatas = categorias_ingreso if tipo_movimiento == "abono" else categorias_gasto
    for categoria, keywords in candidatas.items():
        if any(kw in desc for kw in keywords):
            return categoria
    return "OTHER_INCOME" if tipo_movimiento == "abono" else "OTHER_EXPENSE"


def agregar_categoria(df: pd.DataFrame, categorias_gasto=CATEGORIAS_GASTO,
                       categorias_ingreso=CATEGORIAS_INGRESO) -> pd.DataFrame:
    df = df.copy()
    df["categoria"] = df.apply(
        lambda fila: categorizar(fila["DESCRIPCION"], fila.get("tipo_movimiento", "cargo"),
                                  categorias_gasto, categorias_ingreso),
        axis=1,
    )
    return df


def agregar_fecha(df: pd.DataFrame, anio_defecto: int) -> pd.DataFrame:
    df = df.copy()
    if "FECHA" in df.columns:
        # `.dt.normalize()` deja la fecha sin hora
        # permite agrupar por día para cruzar con feriados
        df["FECHA"] = parsear_fechas(df["FECHA"], anio_defecto).dt.normalize()
    else:
        df["FECHA"] = pd.NaT
    return df


def agregar_feriados(df: pd.DataFrame, pais: str = "CL") -> pd.DataFrame:
    df = df.copy()
    fechas_validas = df["FECHA"].dropna() if "FECHA" in df.columns else pd.Series([], dtype="datetime64[ns]")
    if _holidays is None or fechas_validas.empty:
        df["FERIADO"] = False
        return df
    anios = range(int(fechas_validas.dt.year.min()), int(fechas_validas.dt.year.max()) + 1)
    try:
        feriados = _holidays.country_holidays(pais, years=anios)
        df["FERIADO"] = df["FECHA"].isin(pd.to_datetime(list(feriados.keys())))
    except Exception:
        # Si el pais no es soportado por holidays
        df["FERIADO"] = False
    return df


# insertar en DB
COLUMNAS_FINALES = ["FECHA", "DESCRIPCION", "CATEGORIA", "TIPO_MOVIMIENTO",
                    "MONTO", "SALDO", "FERIADO"]


def finalizar_esquema(df: pd.DataFrame) -> pd.DataFrame:
# Deja el DataFrame con las columnas finales
    if df.empty:
        return pd.DataFrame(columns=COLUMNAS_FINALES)
    salida = pd.DataFrame(index=df.index)
    salida["FECHA"] = df.get("FECHA")
    salida["DESCRIPCION"] = df.get("DESCRIPCION")
    salida["CATEGORIA"] = df.get("categoria")
    salida["TIPO_MOVIMIENTO"] = df.get("tipo_movimiento")
    salida["MONTO"] = df.get("monto")
    salida["SALDO"] = df["SALDO"] if "SALDO" in df.columns else np.nan
    salida["FERIADO"] = df.get("FERIADO")
    return salida[COLUMNAS_FINALES].reset_index(drop=True)


def procesar_cartola(ruta: str, anio_defecto: int | None = None,
                     pais: str | None = None,
                     categorias_gasto=CATEGORIAS_GASTO,
                     categorias_ingreso=CATEGORIAS_INGRESO):
    reporte = {"archivo": Path(ruta).name, "estado": "ok", "avisos": [],
               "pais": None, "anio": None, "fila_encabezado": None,
               "filas_crudas": 0, "filas_validas": 0, "filas_descartadas": 0}
    try:
        raw = leer_archivo(ruta)
    except Exception as e:
        reporte["estado"] = "error"
        reporte["avisos"].append(f"No se pudo leer el archivo: {e}")
        return pd.DataFrame(), reporte

    reporte["filas_crudas"] = int(raw.shape[0])
    pais = pais or detectar_pais(raw)
    anio_defecto = anio_defecto or inferir_anio(raw, ruta)
    reporte["pais"], reporte["anio"] = pais, anio_defecto

    fila = detectar_fila_encabezado(raw)
    if fila is None:
        reporte["estado"] = "error"
        reporte["avisos"].append("No se detecto una fila de encabezado valida (>=3 conceptos).")
        return pd.DataFrame(), reporte
    reporte["fila_encabezado"] = fila

    df = extraer_tabla(raw, fila)
    df = normalizar_montos(df)
    df = agregar_fecha(df, anio_defecto)

    n_antes = len(df)
    df = limpiar_dataframe(df)
    reporte["filas_descartadas"] = int(n_antes - len(df))

    df = agregar_descripcion_limpia(df)
    df = agregar_categoria(df, categorias_gasto, categorias_ingreso)
    df = agregar_feriados(df, pais)
    df = finalizar_esquema(df)          # deja SOLO las columnas finales en MAYÚSCULAS

    reporte["filas_validas"] = int(len(df))
    if df.empty:
        reporte["estado"] = "vacio"
        reporte["avisos"].append("No quedaron movimientos válidos tras la limpieza.")
    return df, reporte


# =============================================================================
# CLI -- invocable como subproceso desde el backend (Java: ProcessBuilder)
# Contrato completo documentado en docs/contrato-ingesta-cartolas.md
# =============================================================================
import argparse
import json
import sys


def _fila_a_dict(fila) -> dict:
    fecha = fila["FECHA"]
    return {
        "fecha": fecha.strftime("%Y-%m-%d") if pd.notna(fecha) else None,
        "descripcion": None if pd.isna(fila["DESCRIPCION"]) else str(fila["DESCRIPCION"]),
        "categoria": None if pd.isna(fila["CATEGORIA"]) else str(fila["CATEGORIA"]),
        "tipo_movimiento": None if pd.isna(fila["TIPO_MOVIMIENTO"]) else str(fila["TIPO_MOVIMIENTO"]),
        "monto": None if pd.isna(fila["MONTO"]) else float(fila["MONTO"]),
        "saldo": None if pd.isna(fila["SALDO"]) else float(fila["SALDO"]),
        "feriado": bool(fila["FERIADO"]) if pd.notna(fila["FERIADO"]) else False,
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Procesa una cartola bancaria (xlsx/xls/csv/pdf) y emite JSON por stdout."
    )
    parser.add_argument("ruta", help="Ruta al archivo de cartola a procesar")
    parser.add_argument("--anio-defecto", type=int, default=None,
                        help="Año a usar si la cartola no lo especifica (default: se infiere)")
    parser.add_argument("--pais", default=None,
                        help="Código de país ISO para feriados, ej. CL (default: se detecta)")
    args = parser.parse_args()

    try:
        df, reporte = procesar_cartola(args.ruta, anio_defecto=args.anio_defecto, pais=args.pais)
    except Exception as exc:  # cualquier fallo no previsto también sale como JSON, nunca como traceback crudo
        print(json.dumps({
            "status": "error",
            "archivo": args.ruta,
            "mensaje": f"Error no controlado: {exc}",
        }, ensure_ascii=False))
        return 1

    if reporte["estado"] == "error":
        print(json.dumps({
            "status": "error",
            "archivo": reporte["archivo"],
            "mensaje": "; ".join(reporte["avisos"]) or "No se pudo procesar el archivo",
        }, ensure_ascii=False))
        return 1

    salida = {
        "status": "ok",
        "archivo": reporte["archivo"],
        "pais": reporte["pais"],
        "anio": reporte["anio"],
        "filas_crudas": reporte["filas_crudas"],
        "filas_validas": reporte["filas_validas"],
        "filas_descartadas": reporte["filas_descartadas"],
        "avisos": reporte["avisos"],
        "transacciones": [_fila_a_dict(fila) for _, fila in df.iterrows()],
    }
    print(json.dumps(salida, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
