# Specification Document

Please fill out this document to reflect your team's project. This is a living document and will need to be updated regularly. You may also remove any section to its own document (e.g. a separate standards and conventions document), however you must keep the header and provide a link to that other document under the header.

Also, be sure to check out the Wiki for information on how to maintain your team's requirements.

## TeamName

**TimeCoin** (Project_12)

### Project Abstract

<!--A one paragraph summary of what the software will do.-->

*TimeCoin* is a time-based cryptocurrency platform designed for UW-Madison students to exchange services within their campus community. Students can register for an account, receive a TimeCoin wallet, and use the service marketplace to post or purchase offerings such as tutoring, coding help, or other skills. Transactions are recorded on a lightweight blockchain ledger; each transfer is hashed, grouped into blocks, and committed to the chain. The platform handles user authentication via JWT, peer-to-peer coin transfers between wallets, and a full marketplace purchase flow backed by balance validation and atomic transactions.

## Key Features of *TimeCoin*

- **Student Accounts**
  - Secure login & registration
  - Wallet with TimeCoin balance
  - Transaction history

- **Service Marketplace**
  - Post a service with description and price
  - Browse available services
  - Filter/search by category

- **Time-Based Currency**
  - Earn TimeCoins by providing services
  - Spend TimeCoins to receive services
  - Track all transfers transparently

- **Transaction System**
  - Peer-to-peer coin transfers
  - Service completion confirmation

- **Rating and Feedback**
  - Review service providers
  - Build trust on the platform

### Customer

The primary customers of *TimeCoin* are University of Wisconsin-Madison students who are wanting to exhange services within our campus community. Many students have valuable skills to share with others, but don't have a platform that can reliably outsource them. Likewise, there are many students who need academic accomodations who don't know where to find it outside of university offered sessions. 

By using time as the main unit of value in our system, we are effectively buying students more time. Making our platform beneficial for those who need academic support and don't have the time to make university help sessions, or students that have time on their hands and skills that are marketable.

### Specification

<!--A detailed specification of the system. UML, or other diagrams, such as finite automata, or other appropriate specification formalisms, are encouraged over natural language.-->

<!--Include sections, for example, illustrating the database architecture (with, for example, an ERD).-->

<!--Included below are some sample diagrams, including some example tech stack diagrams.-->

#### Technology Stack

Frontend: React.js or Node.js
Backend: Java (potentially with SpringBoot)
Database: MySQL
Security/Authentication: TBD

```mermaid
flowchart RL
subgraph Front End
	A(Javascript: React/Node)
end
	
subgraph Back End
	B(Java: SpringBoot)
end
	
subgraph Database
	C[(MySQL)]
end

A <-->|"REST API"| B
B <--> C
```



#### Database

```mermaid
---
title: TimeCoin Database Overview
---
erDiagram
   
    Customer ||--|| Wallet : owns
    Customer ||--o{ Service : creates
    Customer ||--o{ Transaction : buyer
    Customer ||--o{ Transaction : seller
    Service ||--o{ Transaction : purchased_in

    Customer {
        int customer_id PK
        string name
        string email
        string phone
        string user
        string password

    }

    Wallet {
        int wallet_id PK
        int customer_id FK
        string balance
    }

    Service {
        int product_id PK
        int seller_id FK
        string title
        string description
        decimal price
        string category
        bool is_active
    }

    Transaction {
        int transaction_id PK
        int buyer_id FK
        int seller_id FK
        int amount
        int time
        string status
    }
```

#### Class Diagram

```mermaid
---
title: Class diagram for TimeCoin
---
classDiagram
    class user {
        + register()
        + login()
        + makeService()
        + purchaseService()
    }
    class wallet {
        + getBalance()
    }
    class service {
        + updateService()
        + removeService()
    }
    class transaction {
        + process()
        + ensureBalance()
        + complete()
    }
    user <|-- wallet
    user <|-- service
    user <|-- transaction
```

#### Flowchart

```mermaid
---
title: Sample Program Flowchart
---
graph TD;
    Start([Start]) --> Register[/Register/];
    Register --> Login[Login];
    Login --> Purchase_or_Sell{Purchase or Sell};
    Purchase_or_Sell -->|Purchase| Select_Listing[Select Listing];
    Purchase_or_Sell -->|Sell| Create_Service[/Create Service/];
    Select_Listing --> Insufficient_Funds[Insufficient Funds];
    Select_Listing --> Sufficient_Funds[Sufficient Funds]
    Insufficient_Funds --> Cannot_Purchase[Cannot Purchase];
    Sufficient_Funds --> Complete_Transaction[/Transaction Completed/];
    Complete_Transaction --> End([End]);
    Insufficient_Funds --> End;
    Create_Service --> Description[Enter title, description, price];
    Description --> Display_in_Market([Display Listing]);
```

#### Behavior

```mermaid
---
title: State Diagram - TimeCoin
---
stateDiagram
    [*] --> Ready
    Ready --> Pending : purchase initiated
    Pending --> Failed : insufficient funds
    Pending --> Processing : balance validated
    Processing --> Completed : wallets updated
    Processing --> Failed : system error
    Failed --> Ready : retry
    Completed --> Ready : new transaction
```

#### Sequence Diagram

```mermaid
sequenceDiagram
    participant U as User (Buyer)
    participant F as Frontend
    participant B as Backend API
    participant T as Transaction Service
    participant W as Wallet
    participant DB as Database

    U->>F: Click "Purchase"
    F->>B: POST /purchase(serviceId)

    B->>DB: Get buyer balance
    DB-->>B: Return balance

    alt Insufficient Funds
        B-->>F: Return error message
        F-->>U: Show "Insufficient Funds"
    else Sufficient Funds
        B->>T: Create transaction (pending)
        T->>W: Debit buyer
        T->>W: Credit seller
        W->>DB: Update balances
        DB-->>W: Success
        T->>DB: Save transaction
        DB-->>T: Success
        B-->>F: Return success
        F-->>U: Show confirmation
    end
```
### Standards & Conventions

<!--This is a link to a seperate coding conventions document / style guide-->
[Style Guide & Conventions](STYLE.md)
