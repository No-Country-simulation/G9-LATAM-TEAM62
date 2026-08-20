"""
Explicabilidad barata: reutiliza `category_method` y `category_confidence`,
que Backend ya guarda en cada transacción -- no hace falta infraestructura
nueva, solo agregar lo que ya está.

Uso típico: Backend corre la consulta SQL de abajo (o expone un endpoint
que la ejecute), y le pasa el resultado a `resumen_explicabilidad()` para
armar el texto que ve el usuario.
"""

from dataclasses import dataclass


# ---------------------------------------------------------------------------
# La consulta que agrega lo que ya está guardado -- ningún dato nuevo.
# ---------------------------------------------------------------------------
SQL_RESUMEN_POR_USUARIO = """
SELECT
    category_method,
    COUNT(*)                                   AS cantidad,
    ROUND(AVG(category_confidence), 3)         AS confianza_promedio
FROM transactions
WHERE user_id = :userId
  AND source = 'BANK'
  AND category_method IS NOT NULL
GROUP BY category_method
ORDER BY cantidad DESC
"""

ETIQUETAS_METODO = {
    "EXACT_MAPPING": "ya las conocía por correcciones previas de usuarios",
    "KEYWORD_RULE": "las identificó por palabra clave del comercio",
    "ML_MODEL": "las clasificó el modelo de Machine Learning",
    "FALLBACK": "no logró clasificarlas con seguridad",
}


@dataclass
class ConteoMetodo:
    metodo: str
    cantidad: int
    confianza_promedio: float | None


def resumen_explicabilidad(conteos: list[ConteoMetodo]) -> str:
    """Arma el texto que ve el usuario, a partir del resultado de SQL_RESUMEN_POR_USUARIO."""
    total = sum(c.cantidad for c in conteos)
    if total == 0:
        return "Todavía no hay transacciones bancarias clasificadas para este usuario."

    partes = []
    for c in conteos:
        pct = c.cantidad / total * 100
        etiqueta = ETIQUETAS_METODO.get(c.metodo, c.metodo)
        if c.metodo == "ML_MODEL" and c.confianza_promedio is not None:
            partes.append(f"{pct:.0f}% {etiqueta} (confianza promedio {c.confianza_promedio * 100:.0f}%)")
        else:
            partes.append(f"{pct:.0f}% {etiqueta}")

    cuerpo = "; ".join(partes)
    return f"De tus últimas {total} transacciones bancarias: {cuerpo}."


if __name__ == "__main__":
    # Ejemplo con números de muestra, para ver el resultado sin conectarse a la BD real.
    ejemplo = [
        ConteoMetodo("EXACT_MAPPING", 12, None),
        ConteoMetodo("KEYWORD_RULE", 24, None),
        ConteoMetodo("ML_MODEL", 10, 0.81),
        ConteoMetodo("FALLBACK", 4, None),
    ]
    print(resumen_explicabilidad(ejemplo))
