# Rama de Data Science — solo nuestras modificaciones

**Distinto del `guia-branch-y-pr.md` anterior:** aquella rama incluía código Java (registro manual, motor de recomendaciones, historial de perfil) que nosotros mismos habíamos escrito para que Backend lo revisara. **Esta rama es más chica a propósito** — separando aguas, como decidieron: nosotros entregamos el script y el contrato; Backend construye su propio endpoint encima. Ni un archivo `.java` se toca acá.

## 1. Archivos que entran en esta rama

```
data-science/
├── analisis_cartola.ipynb        (ya actualizado en la entrega anterior)
├── procesar_cartola_cli.py       (nuevo -- el script invocable)
├── generar_dataset.py            (sin cambios, se incluye por completitud)
└── budget_recommendation_engine.py  (sin cambios, se incluye por completitud)

docs/
├── contrato-ingesta-cartolas.md  (nuevo -- el contrato para Backend)
└── actividades-data-science.md   (actualizado con esta entrega)
```

**Nada de `backend/`.** Si Backend ya tiene en curso su propia rama con los cambios que sí escribimos nosotros antes (la del `guia-branch-y-pr.md`), esta es completamente independiente — no debería haber conflictos de merge entre las dos, porque no tocan los mismos archivos.

## 2. Crear la rama

```bash
git checkout main
git pull origin main
git checkout -b feature/data-science-ingesta-cartolas
```

## 3. Commit

Al ser un alcance chico y autocontenido, un solo commit alcanza:

```bash
git add data-science/procesar_cartola_cli.py \
        docs/contrato-ingesta-cartolas.md \
        docs/actividades-data-science.md
git commit -m "feat(data-science): script CLI de ingesta de cartolas + contrato para Backend"
git push origin feature/data-science-ingesta-cartolas
```

## 4. Antes de abrir el PR — probarlo vos mismo, no solo confiar en lo que ya corrí acá

```bash
cd data-science
pip install pandas openpyxl xlrd pdfplumber holidays
python3 procesar_cartola_cli.py "ruta/a/una/cartola/real.xlsx"
```

Debería imprimir un único JSON con `"status": "ok"`. Si tenés a mano las 4 cartolas que ya usamos, probalas todas — es la misma prueba que corrí yo, para que quede validado también de tu lado.

## 5. Descripción sugerida del PR

```markdown
## Qué trae este PR

Script de ingesta de cartolas bancarias, listo para que Backend lo invoque
como subproceso desde un endpoint nuevo. Ver el contrato completo en
`docs/contrato-ingesta-cartolas.md`.

## Qué NO trae (a propósito)

- Ningún archivo de `backend/` — el endpoint, el Dockerfile, y la escritura
  en base de datos quedan del lado de Backend, con el contrato ya
  documentado para que no haya ambigüedad.

## Cómo se probó

Contra las 4 cartolas reales del equipo (Banco Chile, CuentaRUT, Falabella,
Mercado Pago) -- las 4 devuelven `status: ok` con categorías válidas.
También se probó el camino de error (archivo inexistente): JSON limpio,
exit code 1, sin traceback crudo.

## Para Backend

Este PR no requiere ninguna acción inmediata de su parte -- es la base para
que puedan construir el endpoint de carga de cartolas cuando les quede
tiempo, con el contrato ya cerrado de antemano.
```
