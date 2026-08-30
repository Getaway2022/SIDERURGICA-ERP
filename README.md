# SIDERURGICA-ERP

Sistema ERP académico para la gestión comercial, logística y de recursos humanos de una empresa siderúrgica.

## Demo desplegada

- Frontend Angular: [https://siderurgica-erp.vercel.app](https://siderurgica-erp.vercel.app)
- Backend Spring Boot: [https://siderurgica-erp-production.up.railway.app](https://siderurgica-erp-production.up.railway.app)
- Base de datos: PostgreSQL en Neon

> Proyecto demostrativo desarrollado para el curso de Desarrollo Web del séptimo ciclo. No representa un sistema productivo oficial.

## Módulos

- Autenticación y autorización por roles
- Ventas, cotizaciones y pedidos
- Despachos e inventario
- Abastecimiento y proveedores
- Recursos humanos, asistencia y planillas
- Reportes e indicadores
- Administración de usuarios

## Tecnologías

| Capa | Tecnologías | Despliegue |
|---|---|---|
| Frontend | Angular 21, TypeScript, RxJS, SCSS | Vercel |
| Backend | Java 17, Spring Boot, Spring Security, JPA, JWT | Railway |
| Datos | PostgreSQL | Neon |

## Estructura del repositorio

```text
SIDERURGICA-ERP/
├── FRONTEND/   # aplicación Angular
└── BACKEND/    # API REST Spring Boot
```

Consulta la documentación específica:

- [Documentación del frontend](FRONTEND/README.md)
- [Documentación del backend](BACKEND/README.md)

## Ejecución local

Backend:

```powershell
cd BACKEND
$env:DB_URL="jdbc:postgresql://HOST/BASE_DE_DATOS?sslmode=require"
$env:DB_USERNAME="USUARIO"
$env:DB_PASSWORD="CONTRASENA"
$env:JWT_SECRET="SECRETO_DE_AL_MENOS_32_CARACTERES"
mvn spring-boot:run
```

Frontend, en otra terminal:

```powershell
cd FRONTEND
npm ci
npm start
```

- Frontend local: `http://localhost:4200`
- Backend local: `http://localhost:3000`

## Seguridad

El repositorio no contiene credenciales reales. Las claves de Neon y JWT deben configurarse como variables privadas de Railway o variables locales del sistema.
