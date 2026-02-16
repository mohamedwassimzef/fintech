# FinTech Application

A comprehensive Java-based financial technology application that provides insurance contract management, payment processing, budgeting, loans, and transaction handling.

## 📋 Table of Contents

- [Features](#features)
- [Technologies](#technologies)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Integration](#api-integration)
- [Entities](#entities)
- [Contributing](#contributing)

## ✨ Features

### Core Modules

- **Insurance Management**
  - Insured Asset Management (CRUD operations)
  - Insurance Contract Management
  - Contract status tracking (PENDING, ACTIVE, EXPIRED, CANCELLED)
  - Asset-Contract relationship management

- **Payment Processing**
  - Integration with Paymee payment gateway
  - Secure payment creation and processing
  - Payment webhook support
  - Real-time payment status tracking

- **Financial Management**
  - Budget creation and tracking
  - Expense management
  - Transaction history
  - Loan management with repayment schedules

- **User Management**
  - User authentication and authorization
  - Role-based access control
  - User profile management

- **Complaint System**
  - Customer complaint tracking
  - Complaint status management

## 🛠 Technologies

- **Java 17** - Programming language
- **Maven** - Dependency management and build tool
- **MySQL 8.0.33** - Relational database
- **Jackson 2.15.4** - JSON processing
- **Java HTTP Client** - HTTP communication
- **JDBC** - Database connectivity

## 📁 Project Structure

```
fintech/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── tn/esprit/
│   │           ├── app/           # Application entry points
│   │           ├── dao/            # Data Access Objects
│   │           │   ├── BudgetDAO.java
│   │           │   ├── ComplaintDAO.java
│   │           │   ├── CrudInterface.java
│   │           │   ├── ExpenseDAO.java
│   │           │   ├── InsuredAssetDAO.java
│   │           │   ├── InsuredContractDAO.java
│   │           │   ├── LoanDAO.java
│   │           │   ├── RepaymentDAO.java
│   │           │   ├── RoleDAO.java
│   │           │   ├── TransactionDAO.java
│   │           │   └── UserDAO.java
│   │           ├── entities/       # Entity models
│   │           │   ├── Budget.java
│   │           │   ├── Complaint.java
│   │           │   ├── Expense.java
│   │           │   ├── InsuredAsset.java
│   │           │   ├── InsuredContract.java
│   │           │   ├── Loan.java
│   │           │   ├── Repayment.java
│   │           │   ├── Role.java
│   │           │   ├── Transaction.java
│   │           │   └── User.java
│   │           ├── enums/          # Enumeration types
│   │           ├── services/       # Business logic services
│   │           │   └── Payment.java
│   │           ├── tests/          # Test classes
│   │           │   └── PaymentTest.java
│   │           └── utils/          # Utility classes
│   └── test/                       # Unit tests
├── lib/                            # MySQL connector library
├── pom.xml                         # Maven configuration
└── README.md                       # This file
```

## 📋 Prerequisites

- **Java Development Kit (JDK) 17** or higher
- **Apache Maven 3.6+**
- **MySQL Server 8.0+**
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code recommended)

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd fintech
```

### 2. Install Dependencies

```bash
mvn clean install
```

### 3. Build the Project

```bash
mvn compile
```

## 🗄 Database Setup

### 1. Create Database

```sql
CREATE DATABASE fintech;
USE fintech;
```

### 2. Create Tables

#### Users Table
```sql
CREATE TABLE user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Insured Assets Table
```sql
CREATE TABLE insured_asset (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    value DOUBLE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);
```

#### Insured Contracts Table
```sql
CREATE TABLE insured_contract (
    id INT PRIMARY KEY AUTO_INCREMENT,
    contract_number VARCHAR(100) UNIQUE NOT NULL,
    asset_id INT NOT NULL,
    user_id INT NOT NULL,
    start_date DATE,
    end_date DATE,
    premium_amount DOUBLE NOT NULL,
    coverage_amount DOUBLE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_by INT,
    FOREIGN KEY (asset_id) REFERENCES insured_asset(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);
```

## ⚙ Configuration

### Database Connection

Update the database connection settings in `tn.esprit.utils.MyDB`:

```java
private static final String URL = "jdbc:mysql://localhost:3306/fintech";
private static final String USER = "your_username";
private static final String PASSWORD = "your_password";
```

### Payment Gateway Configuration

Update the API key in `tn.esprit.services.Payment`:

```java
private static final String API_KEY = "your_paymee_api_key";
```

**Note:** For production, use environment variables instead of hardcoding credentials.

## 💻 Usage

### Running the Application

```bash
mvn exec:java -Dexec.mainClass="tn.esprit.app.main"
```

### Example: Creating an Insured Contract

```java
InsuredContractDAO contractDAO = new InsuredContractDAO();

InsuredContract contract = new InsuredContract(
    0,                                      // id (auto-generated)
    "IC-2026-" + System.currentTimeMillis(), // unique contract number
    29,                                     // assetId
    127,                                    // userId
    LocalDate.of(2026, 2, 8),              // startDate
    LocalDate.of(2027, 2, 8),              // endDate
    1000.0,                                 // premiumAmount
    50000.0,                                // coverageAmount
    ContractStatus.ACTIVE,                  // status
    LocalDateTime.now(),                    // createdAt
    null                                    // approvedBy
);

boolean created = contractDAO.create(contract);
System.out.println("Contract created: " + created);
```

### Example: Processing a Payment

```java
Payment.createPayment(
    15000,                          // amount in millimes
    "Order #123",                   // note
    "John",                         // firstName
    "Doe",                          // lastName
    "test@paymee.tn",              // email
    "+21611222333",                 // phone
    "https://www.return_url.tn",   // returnUrl
    "https://www.cancel_url.tn",   // cancelUrl
    "https://www.webhook_url.tn",  // webhookUrl
    "244557"                        // orderId
);
```

## 🔌 API Integration

### Paymee Payment Gateway

The application integrates with [Paymee](https://paymee.tn/) payment gateway for processing online payments.

**Sandbox URL:** `https://sandbox.paymee.tn/api/v2/payments/create`

**Features:**
- Secure payment processing
- Real-time payment status
- Webhook notifications
- Return and cancel URL handling

**Testing:**
Use the sandbox environment for testing. Update to production URL for live transactions.

## 📊 Entities

### InsuredAsset
Represents an asset that can be insured (vehicles, properties, etc.)
- **Fields:** id, name, type, value, description, createdAt, userId

### InsuredContract
Insurance contract for an asset
- **Fields:** id, contractNumber, assetId, userId, startDate, endDate, premiumAmount, coverageAmount, status, createdAt, approvedBy

### Budget
User budget management
- **Fields:** id, name, amount, period, userId, createdAt

### Expense
Track user expenses
- **Fields:** id, description, amount, category, date, userId

### Loan
Loan management
- **Fields:** id, amount, interestRate, duration, status, userId, createdAt

### Transaction
Financial transaction records
- **Fields:** id, type, amount, description, date, userId

### User
User account information
- **Fields:** id, username, email, password, createdAt

### Complaint
Customer complaint tracking
- **Fields:** id, subject, description, status, userId, createdAt

## 🧪 Testing

### Run Tests

```bash
mvn test
```

### Run Payment Test

```bash
mvn exec:java -Dexec.mainClass="tn.esprit.tests.PaymentTest"
```

## 🔒 Security Considerations

- **Never commit API keys** to version control
- Use **environment variables** for sensitive data
- Implement **password hashing** (BCrypt recommended)
- Use **prepared statements** to prevent SQL injection
- Implement **HTTPS** for production
- Add **input validation** for all user inputs
- Implement **rate limiting** for API calls

## 📝 Best Practices

1. **Database Connections:** Always close connections in finally blocks or use try-with-resources
2. **Error Handling:** Implement comprehensive error handling and logging
3. **Validation:** Validate all inputs before processing
4. **Documentation:** Keep code well-documented
5. **Testing:** Write unit tests for all DAO methods
6. **Security:** Never expose sensitive data in logs

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- **ESPRIT Team** - *Initial work*

## 🙏 Acknowledgments

- Paymee for payment gateway integration
- MySQL for database management
- Jackson for JSON processing

## 📞 Support

For support, email support@example.com or open an issue in the repository.

---

**Last Updated:** February 11, 2026

