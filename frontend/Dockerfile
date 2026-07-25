# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM docker.io/library/node:22-alpine AS build
WORKDIR /app

# Install dependencies first so this layer is cached unless the lockfile changes.
COPY package.json package-lock.json ./
RUN npm ci

# Copy the rest of the sources and produce the production build in /app/dist.
COPY . .
RUN npm run build

# ---- Runtime stage ----
FROM docker.io/library/nginx:alpine AS runtime

# SPA-aware config: unknown routes fall back to index.html.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
