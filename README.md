# InventIQ

<p align="left">
  <img src="https://img.shields.io/badge/Java-21%20LTS-red?style=for-the-badge&logo=openjdk">
  <img src="https://img.shields.io/badge/Spring%20Boot-Monolith-success?style=for-the-badge&logo=springboot">
  <img src="https://img.shields.io/badge/Status-In%20Development%20(DELIVERABLE%202)-yellow?style=for-the-badge">
  <img src="https://img.shields.io/badge/UNAL-Proyecto%20Universitario-green?style=for-the-badge">
</p>

> ## Ingeniería de Software I (2016701) - Sede Bogotá
>
> Instructor: Magda Lucía Mejía Torres
>
> Deliverable 1 - Requirements & Architecture | Submission Date: 05/10/2026
>
> Deliverable 2 - Increment 1 | In progress
>
> Repository: https://github.com/luduarte23/InventIQ

---

## Description

**InventIQ** is an automated operational decision engine for small retail businesses.

Owners and store managers currently spend hours every week manually checking inventory levels, calculating restocking quantities, and estimating when to order from suppliers where they rely on intuition, spreadsheets, or handwritten lists. This leads to financial losses from expired products, stockouts of popular items, and uncoordinated supplier orders that miss out on volume discounts or free-shipping thresholds.

InventIQ captures sales in real time through barcode scanning with **FIFO inventory deduction**, and continuously analyzes sales history, supplier lead times, available cash flow, and batch expiration dates. With this information, it autonomously determines **optimal reorder points (ROP)**, prioritizes purchases under budget limits, and calculates **dynamic discount scales** for merchandise close to expiring.

---

## Scope

### In scope

- **Real-time sales recording and barcode scanning** via USB scanner (keyboard wedge) or manual input, automatically deducting units from the oldest inventory batch (FIFO).
- **Transaction history** (sales and purchases) as the data source for sales-velocity and turnover analysis.
- **Dynamic Reorder Point (ROP) calculation**, analyzing historical sales velocity, supplier lead times, and safety stock.
- **Supplier order consolidation and drafts**, grouping low-stock products by supplier into structured, editable purchase orders.
- **Minimum Order Quantity (MOQ) optimization**, suggesting additional high-turnover products to reach free-shipping/volume-discount thresholds.
- **Purchase prioritization under a budget limit**, ranking orders by margin and turnover when total cost exceeds the available weekly cash flow.
- **Dynamic expiration-based discounting**, applying progressive discount scales to perishable batches based on expiration date and sales pace.
- **WhatsApp integration (Twilio)**: orders and alerts generated on screen, with optional delivery to the administrator's WhatsApp account. The course uses the Twilio Sandbox; moving to production only requires a paid subscription and account configuration - no code changes.

### Out of scope

- Complex POS peripherals (thermal printers, cash drawers, card readers).
- Customer credit module / "Fiado" risk assessment.
- Multi-branch and multi-warehouse management (single inventory only).
- Native mobile applications and offline operation (server-rendered web app only).
- Alternative product identification mechanisms (requires a standardized barcode).

---

## Stakeholders and User Roles

| Role                    | Description                                                                                          |
| ----------------------- | ---------------------------------------------------------------------------------------------------- |
| Administrator           | Store owner/manager. Accesses the dashboard, configures suppliers, inventory thresholds, and prices. |
| Cashier                 | Store clerk. Uses the sales screen to scan products or enter the code manually.                      |
| Generic Barcode Scanner | Optional USB device; sends the code as text input (keyboard wedge).                                  |
| Twilio (WhatsApp API)   | Formats and delivers system alerts to the administrator's WhatsApp account.                          |

---

## Architecture

**Selected style:** Layered (N-Tier) architecture under the **MVC** pattern, organized into four decoupled layers with dependencies pointing downward only:

1. **Presentation (MVC)** - Controllers/views for POS, purchasing, and administration; MOD-01 intercepts every request for session/role validation.
2. **Application** - Orchestrates use cases through module-specific services (`VentasService`, `ReabastecimientoService`, `DescuentosService`, `ComprasService`).
3. **Domain (pure)** - Business rules (FIFO, ROP, MOQ, budget, discounts), with zero infrastructure dependencies; only defines the interfaces it needs (`RepositorioInventario`, `NotificadorMensajes`).
4. **Infrastructure and Deployment (MOD-06)** - Implements those interfaces: embedded-database persistence, versioned schema migrations, and the Twilio adapter.

In the code, classes use English names. For example, `VentasService` is `SalesService`, `NotificadorMensajes` is `MessageNotifier`, and `RepositorioInventario` is split into one port per aggregate (`ProductRepository`, `BatchRepository`, `SaleRepository`, `PurchaseOrderRepository`...). See [Project Structure](#project-structure).

**Accepted trade-offs:**

- _Horizontal scalability vs. simplicity_ - a monolith sacrifices independent component scaling, accepted because the scope targets single-branch businesses.
- _Technological coupling vs. development speed_ - all layers share one platform, maximizing team velocity over cross-language flexibility.

### Modules

| Module | Name                          | Responsibilities                                                                                 |
| ------ | ----------------------------- | ------------------------------------------------------------------------------------------------ |
| MOD-01 | Security and Access           | Authentication, session security, role-based access control.                                     |
| MOD-02 | POS and Inventory             | Real-time sales processing via barcode, FIFO batch deduction.                                    |
| MOD-03 | Replenishment Engine          | ROP calculation, supplier grouping, MOQ optimization, budget prioritization.                     |
| MOD-04 | Expirations and Pricing       | Monitors batch shelf life, calculates progressive expiration discounts.                          |
| MOD-05 | Purchasing and Administration | Manual parameter adjustments, order receipt confirmation, supplier messages, decision audit log. |
| MOD-06 | Infrastructure and Deployment | Packaging, embedded DB, versioned migrations, single-command run.                                |

### Deployment

_In development!_ The application already runs as a single self-contained JAR (see [Getting Started](#getting-started)).

---

## Tech Stack

| Element        | Technology                                                                                    |
| -------------- | --------------------------------------------------------------------------------------------- |
| Language       | Java 21 (LTS)                                                                                 |
| Framework      | Spring Boot 4 (embedded server)                                                               |
| Views          | Thymeleaf, server-rendered; minimal JavaScript for the scanner, sidebar, table filters and charts |
| Charts         | Chart.js, served from the JAR as a WebJar (no internet needed)                                |
| Build          | Maven, with wrapper (`./mvnw`) included                                                       |
| Database       | Embedded H2 stored in a file, schema via Flyway versioned migrations                          |
| Authentication | Spring Security: session login, BCrypt passwords, CSRF protection, two roles (Administrator / Cashier) |
| External API   | Twilio WhatsApp Sandbox, behind a `MessageNotifier` adapter interface. In Deliverable 2 messages are only written to the log; Twilio is enabled in Deliverable 3, only when credentials are configured |
| Testing        | JUnit 5, AssertJ, ArchUnit, JaCoCo (build fails below 80 % domain coverage)                   |
| CI             | GitHub Actions: `./mvnw verify` on every pull request                                         |

---

## Terminology

| Term                             | Definition                                                                                                                                  |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| **ROP** (Reorder Point)          | Critical inventory level that triggers an automatic purchase suggestion, calculated from daily sales velocity, lead time, and safety stock. |
| **FIFO**                         | First In, First Out - oldest batches are deducted/sold first to avoid expiration losses.                                                    |
| **MOQ** (Minimum Order Quantity) | Minimum units/amount a supplier requires to process an order and grant benefits like free shipping.                                         |
| **POS** (Point of Sale)          | The sales-capture interface where barcode scans directly deduct inventory in real time.                                                     |

---

## AI Usage Declaration

This project used AI assistance during development. How we use it:

- Claude Code works in **Plan or Manual** mode, never in automatic editing: it proposes small changes and the member reads and understands the diff before accepting it.
- Claude Code **never commits or pushes**. Each member commits their own work under their own identity.
- AI-assisted code goes through pull requests like any other code, with tests and a review by another member, and every member must be able to explain it in the individual defense.
- AI never replaces the team's decisions: changes to the domain, the ports or the agreements are written down in `docs/desarrollo/decisiones-pendientes.md` for the four members to review.

| Tool        | Parts                                                                                                                          | Purpose                                                    |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------- |
| Claude      | Deliverable 1                                                                                                                  | Architecture validation against the mandatory stack constraints |
| Claude Code | Project skeleton (`pom.xml`, wrapper, configuration, initial migration, base template, CI, architecture test)                  | Generate the initial structure from the documents in `docs/` |
| Claude Code | Part B: FIFO rule, sales service, persistence, sales and inventory screens, provisional demo data, and their tests             | Implement tasks B1–B8 of the work plan                     |
| Claude Code | Products: GTIN barcode validation, registration rules, catalog service, screens, and their tests                               | Implement product registration (part of D4)                |
| Claude Code | UI (sidebar, styles, icons, home), extended catalog, editing and paused sales, history and analytics with charts, demo data generator, and their tests | UI redesign and sales history requested by the team |
| Claude Code | Complete domain model and ports: purchase orders and their state transitions, settings, audit log, alerts, users, supplier minimum order, and their tests | Implement task A2 of the work plan                         |
| Claude Code | CI improvements (coverage summary per layer, test reports) and the pull request template                                      | Implement task A3 of the work plan                         |
| Claude Code | Login with two roles: Spring Security configuration, user loading, login page, menu by role, 403 page, and their tests | Implement task A7 of the work plan                         |
| Claude Code | Development notes in `docs/desarrollo/` and this README (Windows instructions, troubleshooting, workflow)                     | Keep track of progress and pending decisions; task A4      |

The rules Claude Code follows in this repository are in [`CLAUDE.md`](CLAUDE.md).

---

## Gallery

_In development!_

---

## Getting Started

### Requirements

Only **Java 21** or newer. You don't need to install Maven or a database: the project includes the Maven wrapper and uses an embedded H2 database stored in a file.

Check your version with `java -version`.

### Run the application

```bash
git clone https://github.com/luduarte23/InventIQ.git
cd InventIQ
```

Then start it with the command for your terminal:

| Terminal                     | Command                      |
| ---------------------------- | ---------------------------- |
| Linux / macOS / Git Bash     | `./mvnw spring-boot:run`     |
| Windows PowerShell           | `.\mvnw.cmd spring-boot:run` |
| Windows Command Prompt (cmd) | `mvnw.cmd spring-boot:run`   |

PowerShell needs the `.\` prefix; without it, it answers that `mvnw.cmd` is not recognized.

Open <http://localhost:8080> when the log shows `Started InventiqApplication`. The first run downloads the dependencies, so it takes a few minutes. Stop the application with <kbd>Ctrl</kbd>+<kbd>C</kbd>.

The database is created in `./data/`, and Flyway applies the migrations on startup. To start from scratch, stop the application and delete the `data/` folder.

**With demo data**: 21 products of 11 types, batches that are expired or about to expire, 90 days of simulated sales, and six months of price history. The demo uses its own database (`./data/inventiq-demo`), so it doesn't mix with the normal one.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo            # Linux / macOS / Git Bash / cmd
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"      # Windows PowerShell
```

In PowerShell the quotes are required: without them PowerShell splits the argument at the first dot and the application starts **without** the demo data, with no error.

You can also package the application and run it as a single JAR (in PowerShell use `.\mvnw.cmd`):

```bash
./mvnw package
java -jar target/inventiq-0.1.0-SNAPSHOT.jar
java -jar target/inventiq-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo   # with demo data
```

### Troubleshooting

| Problem                                                          | Solution                                                                                                     |
| ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `JAVA_HOME not found` or `java` is not recognized                | Install a JDK 21 (for example Eclipse Temurin) and check that `java -version` shows 21 or newer.            |
| `./mvnw: Permission denied` (Linux / macOS)                      | Run `chmod +x mvnw` once.                                                                                    |
| `Port 8080 was already in use`                                   | Another program uses port 8080 (on Windows, `Get-NetTCPConnection -LocalPort 8080` tells you which one). Use another port: with Maven add `"-Dspring-boot.run.arguments=--server.port=8081"` (quotes required in PowerShell); with the JAR add `--server.port=8081`. Then open <http://localhost:8081>. |
| The demo screens are empty                                       | In PowerShell, put the `-D…` argument in quotes (see above), or check that you opened the demo run.         |
| `Validate failed` / Flyway error after switching branches        | The local database has migrations from another branch. Stop the app and delete `data/` (it is recreated).   |
| `Database may be already in use`                                 | Another instance of the app is still running with the same database. Stop it first.                         |

### Screens

The user interface is in Spanish. Every screen asks you to sign in first at `/login`:

| User     | Password    | Role          | What they see                                            |
| -------- | ----------- | ------------- | -------------------------------------------------------- |
| `admin`  | `admin123`  | Administrator | Every screen                                             |
| `cajero` | `cajero123` | Cashier       | Only Ventas (POS); any other screen answers 403 Forbidden |

Change these passwords if the application is deployed anywhere other than your own computer. Sign out with the **Salir** button in the top bar.

The administrator reaches every screen from the sidebar:

| Screen       | URL               | What it does                                                                      |
| ------------ | ----------------- | --------------------------------------------------------------------------------- |
| Inicio       | `/`               | Today's sales, inventory indicators and batches that need attention               |
| Ventas (POS) | `/pos`            | Sell with a USB barcode scanner or by typing the code; stock is deducted FIFO     |
| Inventario   | `/inventory`      | Stock per product, batches in FIFO order, expiration by day/month/year with colors |
| Productos    | `/admin/products` | Register products by scanner or by hand, edit them, pause or resume their sale    |
| Historial    | `/admin/history`  | Sales history, purchase and sale prices, and interactive charts                   |


Demo barcodes to try, and the current status of every module, are in [`docs/desarrollo/estado-actual.md`](docs/desarrollo/estado-actual.md).

### Run the tests

```bash
./mvnw test                                        # all tests
./mvnw test -Dtest=FifoAllocatorTest               # one class
./mvnw test -Dtest=FifoAllocatorTest#methodName    # one method
./mvnw verify                                      # tests + domain coverage check (fails below 80 %)
```

In Windows PowerShell use `.\mvnw.cmd` and quote the `-D` arguments, for example `.\mvnw.cmd test "-Dtest=FifoAllocatorTest"`.

Run `./mvnw verify` before opening a pull request: it is the same command CI runs. The tests don't need the application running, and they use in-memory databases, so they never touch `data/`.

The coverage report is generated at `target/site/jacoco/index.html`. In a pull request, the CI run summary shows the coverage per layer, and the full report can be downloaded as the `jacoco-report` artifact.

The business rules (`domain` package) are tested without Spring or a database. An ArchUnit test fails if the domain imports Spring or JPA, or if a layer depends on another one in the wrong direction.

### Project Structure

A single Maven project organized by layers (`co.edu.unal.inventiq`):

| Package          | Contents                                                     |
| ---------------- | ------------------------------------------------------------ |
| `domain`         | Model, business rules and ports. Plain Java.                 |
| `application`    | Use cases that orchestrate the domain.                       |
| `infrastructure` | JPA persistence, notifications, security and configuration.  |
| `web`            | Controllers and Thymeleaf views.                             |

Dependencies point one way only: `web → application → domain ← infrastructure`. Each package has a `package-info.java` describing what goes in it.

Development notes (in Spanish) are in [`docs/desarrollo/`](docs/desarrollo/README.md): status, log, design decisions, and the decisions that are still pending.

### Development Workflow

Rules:

- One branch per task, named after the task ID in the work plan: `feature/B2-fifo`.
- Each member commits under their own identity (check it with `git config user.name` and `git config user.email`).
- Everything enters `main` through a pull request approved by another member; nobody pushes to `main` directly. CI runs `./mvnw verify` on every pull request and posts a coverage table per layer in the run summary; `main` is protected so a pull request cannot be merged until the `CI / build` check passes.
- Each pull request is reviewed by a different member each time, so everyone knows code they didn't write.
- Pull requests start from a template (`.github/pull_request_template.md`) with the team's checklist.
- Changing the `domain` package, the port interfaces or `CLAUDE.md` requires the agreement of all four members.
- Migrations are named with date and time (`V20261022_1430__description.sql`) and are never edited once merged.

Step by step for a task:

```bash
git switch main
git pull                                  # start from the latest main
git switch -c feature/A4-readme           # one branch per task
# ... work, then check everything passes:
./mvnw verify
git add <files>
git commit -m "docs(A4): ..."             # your own commit, under your identity
git push -u origin feature/A4-readme
```

Then open the pull request on GitHub against `main`, fill in the template, ask another member for review, and merge only when CI is green and the review is approved. After merging, delete the branch and `git switch main && git pull` before starting the next task.

---

## Team

| Name                              | Email                   |
| --------------------------------- | ----------------------- |
| Luis Alejandro Duarte Daza        | luduarte@unal.edu.co    |
| Santiago Andrés Amézquita Becerra | samezquitab@unal.edu.co |
| Miguel Ángel Suárez Montiel       | migsuarezmo@unal.edu.co |
| Sebastián Camilo Parra Siabato    | separras@unal.edu.co    |
