# Finance AI — Frontend

Cliente web de Finance AI: registro/login, carga de transacciones (a mano o subiendo una
cartola bancaria), historial de transacciones, recomendaciones presupuestarias y perfil
financiero del usuario. Consume la API REST del backend (`../backend`).

## Tecnologías

| Área | Stack |
|---|---|
| Framework | React 19 + Vite |
| Lenguaje | TypeScript |
| Estilos | Tailwind CSS 4 |
| Routing | React Router 7 |
| Formularios | react-hook-form + Zod |
| Tests | Vitest + Testing Library |
| Linting | oxlint |

## Ejecución

Desde la carpeta `frontend/`:

```bash
npm install
npm run dev        # levanta en http://localhost:5173, con proxy de /api hacia el backend
npm run test        # corre la suite de tests (vitest)
npm run build        # type-check (tsc -b) + build de producción
npm run lint        # oxlint
```

El backend tiene que estar corriendo en `http://localhost:8080` (perfil `dev`, ver
`backend/README.md`) — el dev server de Vite proxea `/api` hacia ahí (`vite.config.ts`), así
que en desarrollo el frontend nunca pega directo a otro origen ni necesita configurar la URL
base del backend en ningún lado.

## Estructura

```
src/
  app/            router (react-router) y guard de rutas autenticadas
  components/
    analysis/     piezas del wizard de carga manual (fila de transacción, donut, etc.)
    auth/         formularios de login/registro
    landing/      secciones de la landing pública
    layout/       header, menú de cuenta, menú mobile
    ui/           componentes de UI genéricos (Button, Modal, TextField, Badge, etc.)
  context/        AuthContext, AnalysisContext, ToastProvider — estado global vía React Context
  lib/
    api/          un archivo por recurso del backend (auth, users, transactions, recommendations)
                  + client.ts (wrapper único de fetch)
    validation/   schemas de Zod para cada formulario
  pages/          una carpeta por sección (auth, account, transactions, recommendations, analysis)
  test/           setup de Vitest
```

## Arquitectura

**Cliente HTTP único** (`lib/api/client.ts`): todas las requests pasan por `api.get/post/put/delete/postForm`,
que arma el header `Authorization: Bearer <token>`, maneja el `Content-Type` (excepto en
`FormData`, para no romper el boundary del multipart), y centraliza el manejo de errores:

- El backend devuelve errores en dos formatos distintos (`{"error": "..."}` para errores de
  negocio, `{"campo": "mensaje"}` para fallos de validación de Bean Validation) — `extractErrorMessage`
  entiende ambos.
- Un `401` o `403` con token adjunto (JWT vencido/inválido — este backend no tiene
  `AuthenticationEntryPoint` propio, así que responde 403 para eso, no 401) fuerza logout +
  redirect a `/login` automáticamente.

**Auth**: token y usuario se guardan en `sessionStorage` (no `localStorage`, no cookies —
por pestaña, se pierde al cerrarla). `AuthContext` expone `login/register/logout/updateUser`.

**Toasts**: `ToastProvider` (envuelve toda la app en `App.tsx`) expone `useToast().showToast(mensaje, tipo?)`
para feedback de acciones (éxito/error), consistente en toda la app.

**Un archivo de API por recurso del backend** (`lib/api/*.ts`): cada uno exporta sus tipos
TypeScript (reflejando el DTO/entidad real del backend, no lo que "debería" ser) y sus funciones
de request. Si el backend cambia un contrato, el archivo correspondiente es el único lugar a tocar.
