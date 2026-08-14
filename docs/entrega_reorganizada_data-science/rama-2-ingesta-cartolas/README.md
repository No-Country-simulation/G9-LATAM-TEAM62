# Rama 2 — Contrato de ingesta de cartolas

**Cómo usar esta carpeta:** acá NO hay ningún archivo Java — a propósito. Backend construye el endpoint; nosotros entregamos el script probado y el contrato exacto.

**Guía completa de commit y descripción de PR:** `../docs/guia-branch-data-science.md`
**El contrato en sí (léelo primero):** `contrato-ingesta-cartolas.md`

## Qué trae

- `data-science/procesar_cartola_cli.py` — el script, invocable por línea de comandos, probado contra los 4 bancos reales
- `data-science/analisis_cartola.ipynb` — el notebook del que salió el script (mismo pipeline, formato interactivo)
- `contrato-ingesta-cartolas.md` — formato exacto de entrada/salida, cómo invocarlo desde Java, qué necesita el Dockerfile

## Qué NO trae, a propósito

Ningún endpoint, ningún cambio a `Dockerfile`, ninguna escritura en base de datos — todo eso es responsabilidad de Backend, con el contrato ya cerrado para que no haya ambigüedad.
