# Aarthik Uday: Comprehensive Practical Viva Preparation Guide

This guide serves as a comprehensive study reference for your practical exam. It explains the design, database architecture, backend logic, and architectural details of the **Aarthik Uday** application, structured to help you answer examiner questions confidently.

---

## 1. High-Level System Architecture (MVC)

Aarthik Uday is built using a **decoupled Model-View-Controller (MVC)** architecture:
* **Backend (Spring Boot)**: Acts as a RESTful API provider (Controller + Service + Repository layers).
* **Frontend (Single Page Application - SPA)**: Built with vanilla HTML5, CSS3, and JavaScript (vanilla AJAX), interacting dynamically with the backend.

```
       [ Client Browser (SPA / JS) ]
                    │  ▲
    HTTP JSON Request│  │ JSON Response
                    ▼  │
      ┌───────────────────────────────┐
      │      Spring Boot Backend      │
      │                               │
      │  [ Controllers ] (REST APIs)   │
      │         │                     │
      │         ▼                     │
      │  [ Services ] (Business Logic)│
      │         │                     │
      │         ▼                     │
      │  [ Repositories ] (JPA/SQL)   │
      └─────────┬─────────▲───────────┘
                │         │
                ▼         │ JDBC
         ┌────────────────────────┐
         │  PostgreSQL Database   │
         └────────────────────────┘
```

---

## 2. Database Architecture & Schema Design

The application uses an RDBMS schema managed via Hibernate/JPA.

### Database Tables & Relationships
1. **`users`**: Stores login credentials, roles, and status.
2. **`clients`**: Stores client profile metrics (PAN, Phone, Address) and portfolio aggregates (Total Invested, Current Value, Profit). There is a **One-to-One (`1:1`)** relationship between `users` and `clients`.
3. **`investments`**: Stores granular holdings (Mutual Funds, Stocks, Insurance). There is a **Many-to-One (`N:1`)** relationship linking multiple investments to a single client.
4. **`transactions`**: Stores transaction logs (buys/sells) for auditing. Linked **Many-to-One (`N:1`)** to a client.

```
  ┌──────────────┐          ┌──────────────┐
  │    users     │          │   clients    │
  ├──────────────┤          ├──────────────┤
  │ id (JPA PK)  │ 1 ──── 1 │ id (PK)      │
  │ user_id (Unique)        │ user_id (FK) ───► References users(srl_no)
  │ password     │          │ phone_number │
  │ role         │          │ pan_number   │
  │ status       │          │ address      │
  │ srl_no (DB PK)          │ total_invest │
  └──────────────┘          │ current_val  │
                            │ total_profit │
                            └──────┬───────┘
                                   │ 1
                                   │
                                   │ N
                            ┌──────▼───────┐
                            │ investments  │
                            ├──────────────┤
                            │ id (PK)      │
                            │ client_id(FK)│
                            │ partner      │
                            │ type         │
                            │ scheme_name  │
                            │ units        │
                            │ buy_price    │
                            │ current_price│
                            │ invested_amt │
                            │ current_val  │
                            │ profit_amt   │
                            └──────────────┘
```

### Key Database Schema Constraints
* **Primary Key Mismatch**: In the physical PostgreSQL database, the `users` table's primary key is defined on the **`srl_no`** column (`users_pkey`). However, in Java (`User.java`), the entity primary key `@Id` is mapped to the `id` column.
* **Foreign Key Reference**: The `clients` table's `user_id` column has a foreign key constraint (`fktiuqdledq2lybrds2k3rfqrv4`) referencing **`users(srl_no)`**, not `users(id)`.
* **Null Constraints**: The `srl_no` column in the database has a `NOT NULL` constraint.

---

## 3. Backend Layer-by-Layer Logic

### 3.1 Repository Layer (Data Access)
We extend `JpaRepository<T, ID>` (from Spring Data JPA).
- **`UserRepository`**: Handles user account retrieval and existence checks.
- **`findMaxSrlNo()` Query**: Since `srl_no` is `NOT NULL` and does not have an database-level autoincrement sequence, we define a custom JPQL query to retrieve the current maximum value:
  ```java
  @Query("SELECT COALESCE(MAX(u.srlNo), 0) FROM User u")
  int findMaxSrlNo();
  ```

### 3.2 Service Layer (Business Logic)

#### A. Portfolio Calculation Logic (`ClientService`)
To calculate a client's portfolio aggregates, we retrieve their active investments and aggregate them using the formula:
$$\text{Total Invested} = \sum (\text{units} \times \text{buy\_price})$$
$$\text{Current Value} = \sum (\text{units} \times \text{current\_price})$$
$$\text{Total Profit} = \text{Current Value} - \text{Total Invested}$$
These values are saved to the `clients` table, preventing expensive recalculations on every page load.

#### B. Safe Client Creation (`ClientService.java`)
- When creating a client account:
  1. We check if the username or email already exists.
  2. We create a new `User` entity.
  3. We query `userRepository.findMaxSrlNo()` and assign `srlNo = max(srl_no) + 1` to bypass the database `NOT NULL` constraint.
  4. We save the `User` and create a `Client` entity linked to it.
  5. In [Client.java](file:///Users/umeshmenariya_8079/Desktop/Aarthik_Uday/aarthik_uday/src/main/java/com/aarthikuday/model/Client.java), the mapping is configured to join on `srl_no` to align with the database's foreign key check:
     ```java
     @JoinColumn(name = "user_id", referencedColumnName = "srl_no", nullable = false)
     ```

#### C. Bulk Sheet Import (`ImportService`)
- **Excel Upload**: Uses the **Apache POI** library to parse uploaded `.xlsx` binary streams, reading rows, mapping columns, creating missing users/clients, logging investments, and updating portfolio aggregates in a single transaction.
- **Google Sheets Sync**: Transforms standard edit URLs into CSV exports:
  `https://docs.google.com/spreadsheets/d/{id}/export?format=csv`
  It streams this CSV directly over HTTP, parses the CSV tokens, and processes them identical to the Excel uploads.

### 3.3 Controller Layer (REST APIs)
Handles incoming HTTP requests and returns JSON payloads.
- Annotations: `@RestController`, `@GetMapping`, `@PostMapping`, `@RequestBody`, `@RequestParam`.
- Instead of returning HTML pages (like traditional MVC with Thymeleaf/JSP), we return `ResponseEntity<T>`, returning pure JSON. This allows the client-side JavaScript to render elements dynamically without page refreshes.

### 3.4 Security Configuration (`SecurityConfig`)
- Uses **Spring Security 6.x** chain.
- **Authentication**: Handled via custom JSON entry points instead of standard redirects. If login succeeds, it writes a `200 OK` JSON success message. If login fails, it returns a `401 Unauthorized` JSON.
- **Authorization**: Uses path-based request matchers:
  ```java
  .requestMatchers("/api/admin/**").hasRole("ADMIN")
  .requestMatchers("/api/**").authenticated()
  ```
- **Password Hashing**: Uses `BCrypt` but includes a fallback encoder that allows comparing plain-text credentials for legacy database accounts (`bank123` / `cust123`).

---

## 4. Frontend Architecture & Brand Visual System

Located in `src/main/resources/static/`:
- **`index.html`**: SPA layout with responsive grids, modal cards, and Ionicons vector symbols.
- **`style.css`**: Configures the brand's visual identity:
  - **Colors**: Set variables to brand specifications: Primary Orange (`#FF9505`), Deep Navy Blue (`#050816`), and Translucent Slate Blue (`rgba(13, 18, 43, 0.65)`).
  - **Glassmorphism**: Applied using CSS properties `backdrop-filter: blur(16px)` and translucent card borders to create a premium feel.
- **`app.js`**: Controls rendering, views routing, dynamic role toggles, AJAX network requests, and Chart.js graphics.

### Standalone Client View
- Added a dedicated **Client** tab in the sidebar navigation (visible only to `ROLE_ADMIN`).
- Implemented a standalone clients directory (`view-admin-clients`) with an interactive search filter mapping to client name, PAN ID, or username.
- Tied transition links so that when an admin views a client's detail sheet, the **Client** navigation item remains highlighted in the sidebar.

### SVG brand Logo
- Replaced the text placeholder with an inline vector `<svg viewBox="0 0 100 100">` representing the firm's logo:
  - `<circle>`: Orange backdrop circle (`#FF9505`).
  - `<g fill="#0A0E2B">`: 5 vertical growth bars in deep navy.
  - `<path>` & `<polygon>`: A thick white trending-up line and sharp arrowhead pointing up-right.

---

## 5. Common Examiner Questions & Answers (VIVA Prep)

#### Q1: What is the benefit of Spring Boot over standard Spring?
**A**: Spring Boot provides **starter dependencies** (which bundle library versions to prevent dependency conflicts), **auto-configuration** (which configures Spring beans automatically based on classpath libraries), and an **embedded web server (Tomcat)**, allowing the application to run as a standalone jar.

#### Q2: How did you handle the database mismatch where `srl_no` was the primary key but JPA used `id`?
**A**: The database table was pre-existing with `srl_no` as the primary key. JPA mapped the entity primary key to `id` and joined the `Client` using `user_id`. I resolved the resulting conflicts by:
1. Creating a `findMaxSrlNo()` query in the repository to manually calculate and assign incremented serial numbers to new users, satisfying the database `NOT NULL` constraint.
2. Setting `referencedColumnName = "srl_no"` on the `@JoinColumn` inside the `Client` entity, ensuring that Hibernate inserts the user's serial number into `clients.user_id` instead of its JPA `id`, resolving foreign key constraint violations.

#### Q3: Why did you mark the `active` property as `@Transient` in the `User` class?
**A**: The database schema had a `status` column containing strings (e.g. `'ACTIVE'`, `'INACTIVE'`), but no `active` column. Marking the field as `@Transient` tells Hibernate not to look for an `active` column in the database. Instead, the getter/setter methods translate the boolean state to the string value in the `status` column dynamically.

#### Q4: How do you handle password encoding for older database users who had plain text passwords?
**A**: In `SecurityConfig`, I defined a custom `PasswordEncoder` bean. It checks if the database password starts with a BCrypt prefix (`$2a$`, `$2b$`). If yes, it performs BCrypt comparison. If no, it falls back to a clean string comparison. This ensures backward compatibility for older, unhashed database passwords while securing new registrations with BCrypt.

#### Q5: Why did you use SVG for the logo instead of an image file (like png/jpg)?
**A**: SVGs (Scalable Vector Graphics) are XML-based vector drawings. They do not lose quality when scaled up or down, making the logo look razor-sharp on high-resolution screens (like Retina displays). Additionally, inline SVGs don't require an extra HTTP network request, improving page load speed, and can be styled dynamically using CSS.

#### Q6: How are Excel imports handled on the backend?
**A**: We use **Apache POI**'s `XSSFWorkbook` API to stream the binary data of the uploaded file. We iterate over the rows, read cells, parse usernames and numbers, and save them. We wrap this method in a `@Transactional` block, ensuring that if any row is corrupted, the entire import rolls back to preserve database integrity.
