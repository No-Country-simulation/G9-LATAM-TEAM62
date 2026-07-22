# Finance AI — Frontend

Interfaz web para **Finance AI**, que consume la API REST del [backend](../backend/README.md) para mostrarle al usuario su perfil financiero, el resumen de sus transacciones y las recomendaciones generadas.

Este proyecto solo se encarga de la interfaz: no calcula perfiles ni recomendaciones, esa lógica vive en el backend y en la app externa que lo alimenta.

## Tecnologías

| | |
|---|---|
| Librería UI | React 19 |
| Lenguaje | TypeScript |
| Build tool / dev server | Vite |
| Linter | Oxlint |
| Gestor de paquetes | npm |

## Ejecución

Desde la carpeta `frontend/`:

```bash
npm install       # instala las dependencias (solo la primera vez o si cambia package.json)
npm run dev       # levanta el servidor de desarrollo en http://localhost:5173
npm run build     # compila TypeScript y genera el build de producción en dist/
npm run preview   # sirve localmente el build de producción, para probarlo antes de deployar
npm run lint      # corre el linter (Oxlint) sobre el código
```

## Estructura

```
frontend/
├── public/           # archivos estáticos servidos tal cual (favicon, íconos)
├── src/
│   ├── assets/       # recursos importados desde el código (imágenes, etc.)
│   ├── App.tsx        # componente raíz de la app
│   ├── main.tsx        # punto de entrada: monta App en el DOM
│   └── index.css        # estilos globales
├── index.html        # HTML raíz que carga la app
├── vite.config.ts    # configuración de Vite (plugins, dev server)
├── tsconfig*.json     # configuración de TypeScript
└── .oxlintrc.json     # configuración del linter
```
