# ERP Siderúrgica Perú

Aplicación web para administrar los principales procesos de una empresa siderúrgica. Este repositorio contiene el frontend del caso académico y está preparado como proyecto de portafolio para prácticas preprofesionales.

## Funcionalidades

- Autenticación JWT, renovación de sesión y rutas protegidas.
- Acceso por roles: administración, ventas, almacén, RR. HH. y consulta.
- Navegación con rutas hijas protegidas y carga diferida por módulo.
- Gestión de ventas, cotizaciones, pedidos, despachos e inventario.
- Abastecimiento, proveedores, personal, planillas y reportes.
- Formularios con validaciones y panel con indicadores del negocio.

## Tecnologías

Angular 21, TypeScript 5.9, RxJS, SCSS, Vitest y jsPDF.

## Requisitos

- Node.js 20.19 o superior (se recomienda una versión LTS compatible con Angular 21).
- npm 11.
- Backend REST disponible en `http://localhost:3000`.

## Ejecución local

```bash
npm ci
npm start
```

La aplicación estará disponible en `http://localhost:4200`. La URL del API de desarrollo se configura en `src/environments/environment.ts`; antes de desplegar, actualiza `src/environments/environment.prod.ts` con la URL HTTPS del backend.

Las secciones privadas tienen URLs independientes, por ejemplo `/dashboard/ventas`, `/dashboard/inventario` y `/dashboard/rrhh`. El guard de roles bloquea tanto la navegación desde el menú como el acceso directo a una URL no autorizada.

## Comandos

```bash
npm start          # servidor de desarrollo
npm run build      # compilación optimizada
npm run test:ci    # pruebas en una sola ejecución
npm run format     # aplica el formato del proyecto
npm run check      # formato, pruebas y build
```

## Estructura

```text
src/app/
├── core/
│   ├── auth/         # guard, interceptor, modelos y servicio de sesión
│   └── services/     # estado transversal del dashboard
├── features/
│   ├── auth/pages/   # páginas del flujo de autenticación
│   ├── home/         # página pública y sus componentes internos
│   └── dashboard/    # dominios funcionales del ERP
│       ├── ventas/   # componente, plantilla y servicio del dominio
│       ├── pedidos/
│       ├── inventario/
│       └── ...
├── layouts/
│   └── dashboard-layout/ # estructura visual del área privada
└── shared/
    └── validators/   # validadores reutilizables
```

## Seguridad y configuración

El access token se añade mediante un interceptor y el refresh token debe permanecer en una cookie `HttpOnly`, `Secure` y `SameSite` emitida por el backend. No publiques credenciales, tokens ni URLs privadas. La autorización definitiva siempre debe validarse también en el API; ocultar opciones por rol en el frontend no reemplaza ese control.

## Estado del proyecto

Es un caso demostrativo: requiere el backend correspondiente para operar con datos reales. Los nombres y datos empresariales se usan con fines académicos y no representan un sistema productivo oficial.

## Mejoras futuras

- Aumentar pruebas unitarias y agregar pruebas end-to-end.
- Incorporar manejo global de errores y estados vacíos consistentes.
- Desplegar frontend y API con variables de configuración por entorno.

## Autor

Agrega aquí tu nombre, LinkedIn y correo profesional antes de publicar el repositorio.
