# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **non-heritage cultural preservation platform** (非遗文化保护平台) with a Spring Boot 2.7.3 multi-module backend and a Vue 3 + TypeScript + Vite frontend. The application allows inheritors to manage their profiles, knowledge articles, heritage projects, and cultural products, while regular users browse and purchase items or make appointments.

## Development Commands

### Backend (Maven multi-module, Java 17, Spring Boot 2.7.3)

```bash
cd backend
mvn clean compile        # Compile all modules
mvn clean install        # Full build + install to local repo
mvn test                 # Run tests
mvn spring-boot:run -pl code1st-server   # Start the server (port 8080)
```

Lombok annotation processing works in IntelliJ with the Lombok plugin. For command-line builds, Lombok 1.18.44 is required for Java 17+ compatibility and is configured in the root `pom.xml` with `<lombok>1.18.44</lombok>`. The `code1st-server/pom.xml` excludes lombok from the Spring Boot Maven plugin packaging.

### Frontend (Vue 3, Vite)

```bash
cd frontend
npm install              # Install dependencies
npm run dev              # Dev server (default port 5173)
npm run build            # Production build
```

## Architecture

### Backend Modules

The backend is a Maven parent POM with three child modules:

| Module | Purpose |
|--------|---------|
| `code1st-common` | Shared utilities: constants (`MessageConstant`, `RoleConstant`, `StatusConstant`), exceptions (`BaseException` + subclasses), enums (`OperationType`), Jackson config, JWT utilities (`JwtUtil`, `JwtProperties`), `BaseContext` ThreadLocal holder |
| `code1st-pojo` | Data objects only: entities (`User`, `Inheritor`, `Admin`, `Product`, `ProductOrder`, `HeritageProject`, `KnowledgeArticle`, `InheritorProfile`, `AppointmentSlot`, `Employee`), DTOs, VOs. No logic |
| `code1st-server` | Spring Boot application: controllers, services, mappers, interceptors, aspects, config, handlers. Entry point: `code1stApplication.java` with `@MapperScan("com.class242014group06.mapper")` |

### Three-Role Authentication System

The database has three separate tables replacing the old single `employee` table:

- **`user`** — Regular users who browse and purchase products, make appointments
- **`inheritor`** — Non-heritage inheritors who manage cultural products, knowledge articles, heritage projects
- **`admin`** — System administrators who approve inheritor registrations and manage users

Authentication flow:
1. `AuthController` (`/admin/auth/**`) handles login/register for all three roles
2. Login returns a JWT token with `empId` and `role` claims
3. `JwtTokenAdminInterceptor` validates the token on ALL `/admin/**` paths (excluding public paths listed in `WebMvcConfiguration`)
4. On success, interceptor stores `userId` and `role` in `BaseContext` ThreadLocal
5. Controllers retrieve the current user via `BaseContext.getCurrentId()` and `BaseContext.getCurrentRole()`

JWT configuration in `application.yml`:
```yaml
code1st:
  jwt:
    admin-secret-key: itcast
    admin-ttl: 31536000000   # ~1 year
    admin-token-name: token  # HTTP header name
```

### Public vs Protected Routes

Interceptor exclude paths (no auth required):
- `/admin/auth/login`, `/admin/auth/register`, `/admin/auth/register/inheritor`
- `/admin/employee/login`, `/admin/employee/register`, `/admin/employee/register/inheritor` (legacy)
- `/admin/inheritor/profile/public`, `/admin/knowledge/public`, `/admin/project/public`
- `/admin/slot/available`

### Product System

**Important**: There is a single product system for the e-commerce shop:

| | Product (商城商品) |
|---|---|
| **Backend controller** | `ProductController` at `/admin/product` |
| **Database table** | `product` + `product_order` |
| **Frontend API** | `src/api/product.ts` |
| **API functions** | `listProducts`, `getProductById`, `addProduct`, `listMyProducts`, `submitProductOrder`, etc. |
| **Used by** | `StoreView.vue` and user shops |

`ProductController` handles the e-commerce flow: browsing, ordering, order management (ship/complete/cancel).

Note: The `cultural_product` table and all related `CulturalProduct` classes/interfaces have been removed.

### Controller → Service → Mapper Pattern

Mappers use a mix of **XML files** (in `resources/mapper/*.xml`) for complex queries and **annotation-based** `@Select`/`@Insert` on the interface for simple CRUD. Some mappers use `@AutoFill` (custom AOP annotation from `code1st-common`) for automatic `create_time`/`update_time` population.

Existing mappers:
- `UserMapper`, `InheritorMapper`, `AdminMapper` (auth/user management)
- `ProductMapper`, `ProductOrderMapper` (shop e-commerce)
- `HeritageProjectMapper`, `KnowledgeArticleMapper`, `InheritorProfileMapper`, `AppointmentSlotMapper` (content/features)
- `OrderMapper`, `EmployeeMapper` (legacy appointment + legacy employee)

### Frontend Architecture

- **Router** (`src/router/index.ts`): Route guards check `requiresAuth`, `requiresAdmin`, `requiresInheritor` meta flags against `authStore`
- **Auth store** (`src/stores/auth.ts`): Pinia store, token/user persisted in localStorage under `code1st_token` / `code1st_user` keys
- **API layer** (`src/api/`): Each domain has its own file (`auth.ts`, `product.ts`, `order.ts`, etc.). All share an Axios instance from `request.ts` with `baseURL: '/admin'`
- **Request interceptor**: Attaches `token` header from authStore. Response interceptor: `code === 0` treated as business error, HTTP 401 redirects to `/login`
- **Types** (`src/types/api.ts`): Shared TypeScript interfaces for all DTOs, VOs, and `Result<T>` wrapper

### Key Dependencies

- **Backend**: MyBatis Spring Boot 2.2.0, Druid connection pool, PageHelper, Knife4j (Swagger UI at `/doc.html`), Fastjson, JWT (jjwt 0.9.1), Redis, WebSocket, Apache POI, Aliyun OSS, WeChat Pay SDK
- **Frontend**: Vue 3, Vue Router, Pinia, Axios, Vite

### Lombok & Java Version

The project is configured for **Java 17** but has been built with Java 25 in development. Lombok 1.18.44 (set in root `pom.xml` via `<lombok>` property) is required for Java 17+ annotation processing. IntelliJ's built-in Lombok plugin handles this transparently. If command-line `mvn compile` fails with missing getter/setter errors, ensure Lombok annotation processing is active.
