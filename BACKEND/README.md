# SIGCON Backend

API REST del ERP académico Siderúrgica Perú, desarrollada con Spring Boot y Spring Security.

## Despliegue

- API pública: [Railway](https://siderurgica-erp-production.up.railway.app)
- Frontend: [Vercel](https://siderurgica-erp.vercel.app)
- Base de datos: PostgreSQL administrado en Neon
- Estado: desplegado con fines académicos y de portafolio

La raíz del API no contiene una página pública. Los endpoints protegidos requieren un access token JWT.

## Tecnologías

- Java 17
- Spring Boot 3.2
- Spring Security y JWT
- Spring Data JPA
- PostgreSQL
- Maven
- Docker

## Ejecución local

Define las variables requeridas y ejecuta:

```powershell
$env:DB_URL="jdbc:postgresql://HOST/BASE_DE_DATOS?sslmode=require"
$env:DB_USERNAME="USUARIO"
$env:DB_PASSWORD="CONTRASENA"
$env:JWT_SECRET="SECRETO_DE_AL_MENOS_32_CARACTERES"
$env:CORS_ALLOWED_ORIGINS="http://localhost:4200"
$env:AUTH_COOKIE_SECURE="false"

mvn spring-boot:run
```

El servidor local estará disponible en `http://localhost:3000`.

## Variables de producción

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC de PostgreSQL con SSL |
| `DB_USERNAME` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `JWT_SECRET` | Clave para firmar tokens JWT |
| `JWT_EXPIRATION` | Vigencia del access token en milisegundos |
| `JWT_REFRESH_EXPIRATION` | Vigencia del refresh token en milisegundos |
| `CORS_ALLOWED_ORIGINS` | Origen autorizado del frontend |
| `AUTH_COOKIE_SECURE` | Activa cookies `Secure` en HTTPS |
| `PORT` | Puerto proporcionado por la plataforma |

Las credenciales se configuran exclusivamente como variables privadas de Railway. No deben guardarse en GitHub.

## Seguridad

- Contraseñas almacenadas con BCrypt.
- Autenticación mediante access token y refresh token.
- Refresh token almacenado en una cookie `HttpOnly` y `Secure` en producción.
- Autorización por roles: `ADMIN`, `VENTAS`, `ALMACEN`, `RRHH` y `CONSULTA`.
- CORS restringido al dominio configurado.

## Pruebas y compilación

```bash
mvn test
mvn clean package
```

## Estructura

```text
src/main/java/dsw/sigconbackend/
├── controller/   # endpoints REST
├── dto/          # objetos de entrada y salida
├── exception/    # manejo centralizado de errores
├── model/        # entidades JPA
├── repository/   # acceso a PostgreSQL
├── security/     # JWT, filtros y reglas por rol
└── service/      # lógica del negocio
```

## Despliegue en Railway

El repositorio contiene un `Dockerfile`. En Railway se utiliza:

```text
Root Directory: /BACKEND
Builder: Dockerfile
```

Cada cambio enviado a la rama `main` genera un nuevo despliegue automático.

## Alcance académico

Proyecto desarrollado para el curso de Desarrollo Web del séptimo ciclo. Es una demostración académica y no representa un sistema productivo oficial.
