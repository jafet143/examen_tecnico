FROM node:20-alpine AS construccion
WORKDIR /app

ARG VITE_URL_API=http://localhost:8080/gestion-tarjetas
ARG VITE_KEYCLOAK_URL=http://localhost:8180
ARG VITE_KEYCLOAK_REALM=tarjetas
ARG VITE_KEYCLOAK_CLIENTE_ID=frontend-tarjetas
ENV VITE_URL_API=$VITE_URL_API \
    VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL \
    VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM \
    VITE_KEYCLOAK_CLIENTE_ID=$VITE_KEYCLOAK_CLIENTE_ID

COPY package*.json ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=construccion /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
