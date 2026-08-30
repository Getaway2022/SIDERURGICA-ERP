# SigCon Backend — Spring Boot + Spring Security
## Sprints cubiertos: Sprint 1, Sprint 2, Sprint 3

---

## INSTALACIÓN (desde cero)

### 1. Instalar Java 17
- Descargar: https://adoptium.net/temurin/releases/?version=17
- Windows x64 → archivo `.msi`
- Instalar con todos los valores por defecto
- Verificar: `java -version`

### 2. Instalar Maven
- Descargar: https://maven.apache.org/download.cgi
- Elegir: **Binary zip archive** (`apache-maven-3.9.16-bin.zip`)
- Descomprimir en `C:\Program Files\Apache\maven`
- Agregar a variables de entorno del sistema:
  - Variable nueva: `MAVEN_HOME` = `C:\Program Files\Apache\maven`
  - En `Path` agregar: `%MAVEN_HOME%\bin`
- Verificar: `mvn -version`

---

## CONFIGURACIÓN

### application.properties (ya configurado con tu .env)
```
siderurgica_db / postgres / aeamongol123 / puerto 5432
```
Archivo: `src/main/resources/application.properties`

---

## ⚠️ PASO OBLIGATORIO: Migrar passwords a BCrypt

Spring Security NO puede verificar passwords en texto plano.
Debes encriptar los passwords de tu tabla `seguridad.usuario`.

**Opción A — Usar el SQL incluido:**
Abrir `migrar-passwords-bcrypt.sql` en pgAdmin y ejecutarlo.
Edita los UPDATE según tus usuarios reales.

**Opción B — Generar BCrypt manualmente:**
Usar https://bcrypt-generator.com (rounds = 10)
Copiar el hash y hacer UPDATE directo en pgAdmin.

---

## EJECUTAR

```bash
# En la carpeta del proyecto (donde está pom.xml):
mvn spring-boot:run
```

Primera vez: descarga dependencias (~2 min). Luego es rápido.

Servidor listo cuando veas:
```
Started Application in X.XXX seconds
```
URL: http://localhost:3000

---

## ENDPOINTS

| Método | Endpoint | Auth | Sprint |
|--------|----------|------|--------|
| POST | /login | ❌ Público | Sprint 1 |
| GET | /cotizaciones | ✅ VENDEDOR/ADMIN | Sprint 2 |
| POST | /cotizaciones | ✅ VENDEDOR/ADMIN | Sprint 2 |
| PATCH | /cotizaciones/:id/estado | ✅ VENDEDOR/ADMIN | Sprint 2 |
| GET | /pedidos | ✅ VENDEDOR/ADMIN | Sprint 3 |
| POST | /pedidos | ✅ VENDEDOR/ADMIN | Sprint 3 |
| PATCH | /pedidos/:id/estado | ✅ VENDEDOR/ADMIN | Sprint 3 |

---

## CÓMO FUNCIONA SPRING SECURITY (para sustentación)

```
Angular POST /login
       ↓
AuthController recibe { username, password }
       ↓
AuthenticationManager (Spring Security)
       ↓
CustomUserDetailsService.loadUserByUsername()
       ↓ carga usuario de BD
BCryptPasswordEncoder.matches(password, hash)
       ↓ verifica password
Si OK → devuelve { success, usuario: { id, username, rol, estado } }
Si falla → 401 Unauthorized
```

Para /cotizaciones y /pedidos, Spring Security verifica
automáticamente que el usuario tenga rol VENDEDOR o ADMIN.

---

## ESTRUCTURA DEL PROYECTO

```
sigconbackend/
├── pom.xml
├── migrar-passwords-bcrypt.sql   ← EJECUTAR ANTES DE INICIAR
└── src/main/
    ├── resources/
    │   └── application.properties
    └── java/dsw/sigconbackend/
        ├── Application.java
        ├── security/
        │   ├── SecurityConfig.java          ← HU02: reglas de acceso
        │   └── CustomUserDetailsService.java← HU02: carga usuario de BD
        ├── controller/
        │   ├── AuthController.java          ← POST /login
        │   ├── CotizacionController.java    ← HU03, HU05
        │   └── PedidoController.java        ← HU06
        ├── service/
        │   ├── AuthService.java             ← HU02
        │   ├── CotizacionService.java       ← HU03, HU04, HU05
        │   └── PedidoService.java           ← HU06, HU07
        ├── repository/
        │   ├── UsuarioRepository.java
        │   ├── CotizacionRepository.java
        │   └── PedidoRepository.java
        ├── model/
        │   ├── Usuario.java
        │   ├── Rol.java
        │   ├── Cotizacion.java
        │   └── Pedido.java
        └── dto/
            ├── LoginRequest.java
            ├── LoginResponse.java
            ├── CotizacionRequest.java
            ├── PedidoRequest.java
            └── EstadoRequest.java
```

---

## COBERTURA POR HISTORIA DE USUARIO

| HU | Descripción | Archivo |
|----|-------------|---------|
| HU01 | Sistema base e infraestructura | `Application.java`, `application.properties`, `pom.xml` |
| HU02 | Autenticación y acceso | `SecurityConfig.java`, `CustomUserDetailsService.java`, `AuthService.java` |
| HU03 | Registrar cotización | `CotizacionService.registrarCotizacion()` |
| HU04 | Calcular descuentos automáticamente | `CotizacionService.calcularDescuento()` |
| HU05 | Consultar cotizaciones | `CotizacionService.listarCotizaciones()` |
| HU06 | Registrar pedido de venta | `PedidoService.registrarPedido()` |
| HU07 | Validar cliente en venta | `PedidoService.registrarPedido()` (validaciones) |
| HU08 | Generar comprobante de pago | Pendiente — próximo sprint |
