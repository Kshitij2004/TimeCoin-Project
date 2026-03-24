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

| Layer | Technology |
|---|---|
| Frontend | React.js |
| Backend | Java 21 with Spring Boot 4.0.2 |
| Database | MySQL 8.0 (Dockerized) |
| Authentication | JWT via `jjwt` 0.13.0, BCrypt password hashing |
| Security | Spring Security |
| ORM | Hibernate / Spring Data JPA |
| Build Tool | Gradle |
| Testing | JUnit 5 + Mockito |

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
    users ||--|| wallets : owns
    users ||--o{ listings : creates
    users ||--o{ transactions : "purchase history"
    wallets ||--o{ transactions : "sends/receives"
    wallets ||--o{ validators : "stakes as"
    wallets ||--o{ staking_events : "logs"
    blocks ||--o{ block_transactions : contains
    transactions ||--o{ block_transactions : "included in"
    transactions }o--|| blocks : "confirmed in"

    users {
        int id PK
        string username
        string email
        string password_hash
        timestamp created_at
    }

    wallets {
        int id PK
        int user_id FK
        string wallet_address
        string public_key
        decimal coin_balance
        timestamp created_at
    }

    coins {
        int id PK
        decimal total_supply
        decimal circulating_supply
        decimal current_price
        timestamp updated_at
    }

    listings {
        int id PK
        int seller_id FK
        string title
        text description
        decimal price
        string category
        enum status
        string image_url
        timestamp created_at
    }

    transactions {
        int id PK
        string sender_address
        string receiver_address
        decimal amount
        int user_id FK
        string symbol
        enum transaction_type
        decimal price_at_time
        decimal total_usd
        decimal fee
        int nonce
        timestamp timestamp
        string transaction_hash
        enum status
        int block_id FK
    }

    blocks {
        int id PK
        int block_height
        string previous_hash
        string block_hash
        string validator_address
        timestamp timestamp
        int transaction_count
        enum status
    }

    block_transactions {
        int id PK
        int block_id FK
        int transaction_id FK
    }

    validators {
        int id PK
        string wallet_address FK
        decimal staked_amount
        enum status
        timestamp joined_at
        timestamp last_selected_at
    }

    staking_events {
        int id PK
        string wallet_address FK
        enum event_type
        decimal amount
        timestamp created_at
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
