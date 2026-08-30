# ERP Siderúrgica Perú — Frontend

Aplicación web académica para administrar procesos comerciales, logísticos y de recursos humanos de una empresa siderúrgica.

## Despliegue

- Aplicación: [https://siderurgica-erp.vercel.app](https://siderurgica-erp.vercel.app)
- API: [Railway](https://siderurgica-erp-production.up.railway.app)
- Estado: desplegado con fines académicos y de portafolio

## Funcionalidades

- Autenticación JWT y rutas protegidas.
- Acceso por roles: administración, ventas, almacén, RR. HH. y consulta.
- Gestión de ventas, cotizaciones, pedidos, despachos e inventario.
- Abastecimiento y proveedores.
- Personal, asistencia y planillas.
- Indicadores y reportes del negocio.

## Tecnologías

- Angular 21
- TypeScript 5.9
- RxJS
- SCSS
- Vitest
- jsPDF

## Ejecución local

Requisitos: Node.js 20.19 o superior y npm 11.

```bash
npm ci
npm start
```

La aplicación estará disponible en `http://localhost:4200` y utilizará el API configurado en `src/environments/environment.ts`.

## Comandos

```bash
npm start          # servidor de desarrollo
npm run build      # compilación de producción
npm run test:ci    # pruebas en una ejecución
npm run format     # formatear el código
npm run check      # formato, pruebas y build
```

## Configuración por entorno

```text
src/environments/environment.ts       # API local
src/environments/environment.prod.ts  # API desplegada en Railway
```

Las credenciales y secretos nunca deben almacenarse en los archivos de Angular, porque todo el frontend es público en el navegador.

## Estructura

```text
src/app/
├── core/       # autenticación, guards, interceptor y servicios globales
├── features/   # módulos funcionales del ERP
├── layouts/    # estructura visual del dashboard
└── shared/     # validadores y elementos reutilizables
```

## Despliegue en Vercel

```text
Root Directory: FRONTEND
Framework: Angular
Build Command: npm run build
Output Directory: dist/siderurgica-frontend/browser
```

`vercel.json` incluye el rewrite necesario para acceder directamente a las rutas de Angular.

## Alcance académico

Proyecto desarrollado para el curso de Desarrollo Web del séptimo ciclo. Es una demostración académica y no representa un sistema productivo oficial.
