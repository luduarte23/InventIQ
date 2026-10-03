# InventIQ

<p align="left">
  <img src="https://img.shields.io/badge/Java-21%20LTS-red?style=for-the-badge&logo=openjdk">
  <img src="https://img.shields.io/badge/Spring%20Boot-Monolith-success?style=for-the-badge&logo=springboot">
  <img src="https://img.shields.io/badge/Status-In%20Development (DELIVERABLE 1)-yellow?style=for-the-badge">
  <img src="https://img.shields.io/badge/UNAL-Proyecto%20Universitario-green?style=for-the-badge">
</p>

> ## Ingeniería de Software I (2016701) - Sede Bogotá
>
> Instructor: Magda Lucía Mejía Torres
>
> Deliverable 1 - Requirements & Architecture | Submission Date: 05/10/2026
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

_In development!_

---

## Tech Stack

| Element        | Technology                                                                |
| -------------- | ------------------------------------------------------------------------- |
| Language       | Java 21 (LTS)                                                             |
| Framework      | Spring Boot (embedded server)                                             |
| Views          | Thymeleaf (server-rendered, no JavaScript)                                |
| Build          | Maven, with wrapper (`./mvnw`) included                                   |
| Database       | Embedded (H2/SQLite), schema via versioned migrations                     |
| Authentication | Session-based, role-restricted (Administrator / Cashier)                  |
| External API   | Twilio WhatsApp Sandbox, behind a `NotificadorMensajes` adapter interface |

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

This project used AI assistance (Claude) during development for:

- Architecture validation against the mandatory stack constraints

---

## Gallery

_In development!_

---

## Getting Started

_In development!_

---

## Team

| Name                              | Email                   |
| --------------------------------- | ----------------------- |
| Luis Alejandro Duarte Daza        | luduarte@unal.edu.co    |
| Santiago Andrés Amézquita Becerra | samezquitab@unal.edu.co |
| Miguel Ángel Suárez Montiel       | migsuarezmo@unal.edu.co |
| Sebastián Camilo Parra Siabato    | separras@unal.edu.co    |
