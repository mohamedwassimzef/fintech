# 📑 FinTech System Technical Documentation

This document provides a comprehensive overview of the FinTech ecosystem, including the **Java Desktop Application** (Frontend/Desktop Client) and the **Symfony Backend** (API & Webhooks).

---

## 🏗️ 1. Architecture Overview

The system is split into two main components that communicate via a shared MySQL database and external webhooks.

### ☕ Java Desktop Application (`IdeaProjects/fintech`)
- **UI Framework**: JavaFX (FXML).
- **Persistence Layer**: Custom DAO pattern using JDBC.
- **Key Modules**:
    - **Insurance**: Asset management, package browsing, and contract requests.
    - **Personal Finance**: Budgets, expenses, and loans.
    - **Transactions**: Payment processing via Paymee.
    - **Admin Dashboard**: Request approval, package management, and analytics.

### 🐘 Symfony Backend (`dev/finance_app`)
- **Role**: Web API, background processing, and Webhook consumer.
- **Key Features**:
    - **Webhook Receivers**: Handles Paymee (payments) and BoldSign (digital signatures) notifications.
    - **Business Logic Managers**: Pure PHP services in `src/Service/Manager/` that enforce domain rules.
    - **Data Seeding**: Symfony console commands for realistic test data.

---

## 🗄️ 2. Database Synchronization & Integrity

The database uses a strict schema with several mandatory audit columns. 

### Audit Columns
Every transactional table (e.g., `app_transaction`, `insurance_package`, `expense`) requires:
- `created_at` (DATETIME): Automatically set via `NOW()` in DAOs.
- `updated_at` (DATETIME): Updated on every edit via `NOW()`.
- `created_by_id` (INT): Foreign key to `app_user(id)`.

### DAO Implementation Strategy
To prevent **Foreign Key Constraint Violations**, all Java DAOs use a standardized identity resolution pattern:
```java
int createdBy = SessionManager.getInstance().getUserId();
// Fallback to owner if session is unavailable
if (createdBy <= 0) createdBy = entity.getUserId(); 
pstmt.setInt(N, createdBy);
```

---

## 💳 3. Paymee Payment Integration

The system integrates with **Paymee** for secure financial transactions.

### Workflow
1.  **Creation**: Java app creates a `PENDING` record in `app_transaction`.
2.  **API Call**: `Payment.createPayment()` sends request to Paymee.
3.  **Bypassing SSL**: For the **Sandbox environment**, SSL validation is bypassed in Java to prevent `CertificateExpiredException` caused by expired sandbox certs.
4.  **UX**: A dedicated window pops up in the Java app showing the payment link.
5.  **Confirmation**: The Symfony Webhook (`/webhook/paymee`) updates the transaction to `COMPLETED` once the payment is verified.

### Configuration
- **API Key**: Managed in `tn.esprit.services.Payment`.
- **Environment**: Currently configured for `https://sandbox.paymee.tn/api/v2/`.

---

## 📧 4. Email Service Setup

Emails are sent via **Gmail SMTP** using `Jakarta Mail`.

### Gmail Authentication
Because Google has deprecated standard password login, you **MUST** use an **App Password**:
1.  Enable **2-Step Verification** on your Google account.
2.  Generate an **App Password** named "FinTech".
3.  Update the `APP_PASSWORD` constant in `src/main/java/tn/esprit/services/EmailService.java`.

---

## 🛠️ 5. Troubleshooting & Common Fixes

### "Nothing happens after clicking Post Payment"
- **Check Console**: Look for `SSLHandshakeException` or `401 Unauthorized`.
- **Validation**: Ensure all required fields (Name, Phone, Email) are filled.
- **DB Check**: Verify that a row was inserted into `app_transaction` with status `PENDING`.

### "FXMLLoadException" on Dashboard
- **Enum Mismatch**: Often caused by a `RequestStatus` in the database that isn't defined in the Java `tn.esprit.enums.RequestStatus` class.
- **Solution**: Use `RequestStatus.fromString()` which defaults to `PENDING` for unknown values.

### "Foreign Key Constraint Fails"
- **Root Cause**: Trying to save `created_by_id = 1` when no user with ID 1 exists.
- **Solution**: Always use `SessionManager.getInstance().getUserId()`.

---

## 🧪 6. Running Tests

### Symfony Unit Tests
```powershell
php bin/phpunit tests/Service/ --testdox
```

### Java Build Check
Verify your `pom.xml` dependencies are correctly resolved by running:
```powershell
mvn clean compile
```

---
*Documentation last updated: May 14, 2026*
