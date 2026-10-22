# 🚀 Reto Técnico Backend: API Gateway + Microservicios + OAuth 2.0

Solución backend robusta basada en una arquitectura de microservicios, desarrollada para cumplir con los más altos estándares de diseño, principios SOLID, seguridad perimetral y trazabilidad de solicitudes.

---

## 🏗️ Arquitectura del Sistema

El ecosistema está compuesto por los siguientes componentes aislados en una red privada de Docker:
* **API Gateway (Puerto 1000):** Único punto de entrada público del sistema. Se encarga del enrutamiento perimetral, validación centralizada de seguridad y propagación del identificador de rastreo (`X-Trace-Id`).
* **Inventory Service (Puerto 1100 - Privado):** Administra el catálogo de productos y el stock disponible de forma altamente eficiente mediante una arquitectura reactiva basada en Spring WebFlux y R2DBC, respondiendo en tiempo real a las consultas de disponibilidad.
* **Order Service (Puerto 1200 - Privado):** Administra el ciclo de vida de los pedidos, sus estados (`PENDING`, `CONFIRMED`, `CANCELLED`), el registro histórico de cambios y la validación síncrona de inventario.
* **Keycloak (Puerto 8080):** Servidor centralizado de autenticación y autorización bajo el estándar OAuth 2.0 / OpenID Connect con tokens JWT.
* **Bases de Datos:** Instancias independientes de bases de datos relacionales por cada microservicio para garantizar el aislamiento de datos.

---

## 🛠️ Stack Tecnológico

* **Java 21** & **Spring Boot** (Spring Security, Spring Data JPA, WebFlux)
* **Spring Cloud Gateway** (Enrutamiento y filtros reactivos)
* **OAuth 2.0 / JWT** (Protección mediante Keycloak)
* **Spring WebFlux** - Framework reactivo para aplicaciones web
* **Docker & Docker Compose** (Contenerización y orquestación local)
* **OpenAPI / Swagger** (Documentación interactiva de APIs)
* **Logback & AOP** (Trazabilidad y propagación de `X-Trace-Id` en logs y respuestas)
* **Lombok** (Reducción de código boilerplate y gestión limpia de objetos)
* **MapStruct** Mapeo de objetos de alto rendimiento mediante generación de código en tiempo de compilación

---

## 📦 Instrucciones de Ejecución

Para poner en marcha todo el ecosistema desde cero (bases de datos limpias, Keycloak, microservicios y API Gateway):

1. Descomprime el archivo RAR del proyecto en tu directorio de trabajo y ubícate en la carpeta raíz.
2. Ejecuta el siguiente comando para limpiar volúmenes previos y construir los contenedores:
   ```bash
   docker compose down -v
   docker compose up --build

---

## 🔐 Configuración Inicial de Keycloak (Autenticación)

Para poder realizar operaciones protegidas (`POST`, `PUT`, `DELETE`) en la API, es necesario configurar el servidor de autenticación de Keycloak la primera vez que se levanta el entorno:

1. Ingresa a la interfaz de administración de Keycloak: [`http://localhost:8080`](http://localhost:8080)
2. **Credenciales de Administrador inicial:**
    * **Usuario:** `admin`
    * **Contraseña:** `admin`
3. **Crear un Realm:**
    * Crea un nuevo Realm llamado: `postman-realm`
4. **Crear el Cliente (Client):**
    * Dentro del realm, crea un cliente con el ID: `postman-client` con el name `postman-test-client`.
5. **Crear un Usuario de Prueba:**
    * Dirígete a la sección de usuarios y crea uno nuevo con los siguientes datos:
        * **Username:** `testuser`
        * **Email:** `test@test.com`
        * **First Name:** `user`
        * **Last Name:** `test`
        * **Email Verified:** `true`
    * Ve a la pestaña **Credentials** del usuario creado y ainsiéale la contraseña: `123456` (desactivando la opción de *Temporary* si lo deseas).

---

## 📄 Documentación de APIs y Swagger

El API Gateway centraliza el acceso público redirigiendo a los microservicios internos. Las consultas de lectura (`GET`) y la documentación de Swagger son públicas, mientras que las operaciones de modificación (`POST`, `PUT`, `DELETE`) exigen autenticación mediante token JWT.

### 🌐 Endpoints de Swagger (Documentación e Interfaz UI):
* **Order Service Swagger:** [`http://localhost:1000/api/orders/swagger-index-html`](http://localhost:1000/api/orders/swagger-index-html)
* **Inventory Service Swagger:** [`http://localhost:1000/products/swagger-index-html`](http://localhost:1000/inventory/swagger-index-html)

---

## 🧪 Guía Rápida de Uso con Postman

En la raíz del archivo ZIP adjunto encontrarás la colección de Postman (`Technical Challenge - BW.postman_collection.json`) lista para importar.

Todas las peticiones deben realizarse a través del **API Gateway (`http://localhost:1000`)**. Las operaciones de modificación (`POST`, `PATCH`) están protegidas y configuradas en Postman mediante la pestaña **OAuth 2.0**, utilizando la variable de entorno `{{TOKEN}}` con el prefijo **Bearer**.

### 🔑 Autenticación
* **`POST /Authentication`**: Genera y obtiene el token OAuth 2.0 desde Keycloak utilizando las credenciales del usuario de prueba, el cual se almacena automáticamente en la variable `{{TOKEN}}` de la colección.

### 📦 Módulo de Inventario (`/inventory`)
* **`POST /Add Product`**: Registra un nuevo producto con su stock inicial en el sistema. *(Protegido por OAuth 2.0)*
* **`GET /Get All Product`**: Lista todos los productos disponibles en el inventario. *(Público)*
* **`PATCH /Update Stock`**: Modifica o actualiza el stock actual de un producto específico. *(Protegido por OAuth 2.0)*

### 🛒 Módulo de Pedidos (`/order`)
* **`POST /Add Order`**: Crea un nuevo pedido. Dispara la validación síncrona de disponibilidad en el inventario; si hay stock, se confirma automáticamente. *(Protegido por OAuth 2.0)*
* **`GET /Get Order`**: Consulta los detalles de un pedido específico mediante su ID. *(Público)*
* **`GET /Get History Status`**: Permite consultar el historial de cambios de estado asociados a un pedido. *(Público)*
* **`PATCH /Change Status Order`**: Actualiza o cancela el estado de un pedido según las reglas de negocio establecidas. *(Protegido por OAuth 2.0)*
