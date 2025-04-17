# Plataforma SaaS de Reservas Online

Aplicación web modular y evolutiva para gestión de reservas orientada a diversos tipos de negocios (peluquerías, consultorios médicos, restaurantes, hoteles y otros comercios).

## Arquitectura

La plataforma está diseñada siguiendo un enfoque modular evolutivo:

- **Fase 1:** Monolito modular con estructura preparada para futura migración a microservicios
- **Fase 2:** Transición gradual a microservicios independientes
- **Fase 3:** Arquitectura completa de microservicios con integración de servicios externos

## Stack Tecnológico

- **Backend:** Java 17 con Spring Boot 3.2.4+
- **Base de datos:** PostgreSQL con esquemas separados por tenant
- **Seguridad:** Auth0/Firebase Auth para autenticación
- **Almacenamiento:** Local evolucionando a servicios en la nube
- **Notificaciones:** Email (Google Calendar) evolucionando a OneSignal
- **Despliegue:** Railway (optimizado para costos)

## Módulos Principales

### Módulo de Negocios y Configuración
- Gestión de negocios y configuraciones
- Definición de servicios y categorías
- Configuración de campos personalizados

### Módulo de Reservas y Clientes
- Gestión de clientes y profesionales
- Sistema de reservas y disponibilidad
- Integración con Google Calendar
- Sistema de fidelización y referidos por puntos

### Módulo de Identidad y Seguridad
- Autenticación via Auth0/Firebase Auth
- Gestión de roles y permisos
- Multi-tenancy a nivel de esquema

### Módulo de Pagos
- Integración con gateway de pagos
- Gestión de transacciones

### Módulo de Notificaciones
- Notificaciones vía email
- Sistema de alertas internas

## Requisitos

- Java 17+
- PostgreSQL 14+
- Maven 3.6+

## Configuración del Proyecto

### Base de Datos

1. Crear base de datos PostgreSQL:
```sql
CREATE DATABASE booking_platform;
```

2. Ejecutar migraciones con Flyway (automático al iniciar la aplicación o manual):
```bash
mvn flyway:migrate
```

### Variables de Entorno

Crear archivo `.env` o configurar las siguientes variables:

```
# Base de datos
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/booking_platform
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Auth0/Firebase
AUTH0_ISSUER_URI=https://dev-example.us.auth0.com/
JWT_SECRET=your-strong-jwt-secret-key-here-min-32-chars

# Configuración de la aplicación
APP_STORAGE_LOCATION=file:./uploads
```

## Ejecución

### Desarrollo

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Producción

```bash
mvn clean package
java -jar target/booking-platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Estructura del Proyecto

```
booking-platform/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── bookingsaas/
│   │   │           ├── common/         # Código compartido entre módulos
│   │   │           ├── core/           # Núcleo de la aplicación
│   │   │           ├── module/         # Módulos funcionales
│   │   │           │   ├── business/   # Módulo de negocios
│   │   │           │   ├── booking/    # Módulo de reservas
│   │   │           │   ├── identity/   # Módulo de identidad
│   │   │           │   ├── payment/    # Módulo de pagos
│   │   │           │   └── notification/ # Módulo de notificaciones
│   │   │           └── multitenancy/   # Soporte para multi-tenancy
│   │   └── resources/
│   │       ├── application.yml         # Configuración principal
│   │       └── db/migration/           # Migraciones Flyway
│   └── test/                          # Tests
└── pom.xml                            # Configuración de Maven
```

## API REST

La API está documentada con OpenAPI (Swagger) y está disponible en:

```
http://localhost:8080/api/swagger-ui.html
```

## Licencia

[MIT](LICENSE)