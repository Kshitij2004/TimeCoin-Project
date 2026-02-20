# Time-Based Crypto Concept (Name TBD)

## 1. Project Idea

We are building a **time-based cryptocurrency system**.

**1 token = 1 minute of time privilege**

Tokens can be used for:

- Being late to class  
- Skipping time in a line  
- Buying marketplace items  

The system simulates a small controlled **time-based economy** with wallets, transactions, and price changes.

---

## 2. System Architecture Overview

**Frontend:** React application with routing  
**Backend:** Spring Boot REST API  
**Database:** MySQL  
**Infrastructure:** Docker Compose + CI pipeline  

The system includes:

- User authentication
- Wallet + token economy
- Marketplace purchasing
- Transaction ledger
- Token price simulation
- Secure transaction handling

---

## 3. Frontend (User Side)

### 3.1 Authentication Pages

#### Registration Page
- User creates account
- Username / email / password
- Sends request to backend API

#### Login Page
- User logs in
- Backend verifies credentials
- Session or auth token stored locally

---

### 3.2 Dashboard (Logged-In User)

Displays:

- Current wallet token balance
- Current coin price
- Summary of recent transactions

Future:

- Graph of token value over time

---

### 3.3 Wallet Features

Users can:

- View token balance
- View recent transactions
- Spend tokens for time privileges
- Receive tokens after purchase

---

### 3.4 Marketplace Page

Shows list of purchasable items or token purchases.

Each item includes:

- Name
- Token price
- Available quantity

Users can:

- Buy items using tokens
- Purchase tokens through backend API

---

### 3.5 Transaction History Page

Displays:

- All user transactions
- Sender / receiver
- Amount
- Timestamp
- Transaction type (buy, spend, transfer)

---

### 3.6 Token Price Visualization

Simple chart:

- Token value over time
- May depend on supply and demand
- Data fetched from backend price history endpoint

---

## 4. Backend (System Logic)

### 4.1 REST API Endpoints

**Authentication**
- POST /register
- POST /login

**Wallet**
- GET /wallet
- GET /wallet/balance

**Transactions**
- GET /transactions
- POST /transactions/buy
- POST /transactions/spend

**Marketplace**
- GET /products
- POST /products/buy

**Coin Value**
- GET /coin/value
- GET /coin/history

---

### 4.2 Database Schema

**Users**
- id
- username
- email
- password_hash

**Wallets**
- id
- user_id
- balance

**Transactions**
- id
- sender_id
- receiver_id
- amount
- type
- timestamp
- encrypted_signature (placeholder)

**Products**
- id
- name
- token_price
- quantity

**PriceHistory**
- id
- price
- timestamp

---

### 4.3 Crypto & Ledger Placeholder

Each transaction will include:

- Unique transaction ID
- Sender and receiver
- Amount
- Timestamp
- Hash of transaction data

Transactions are stored in a ledger table.

Future extension:

- Group transactions into blocks
- Link blocks using hashes (blockchain-style simulation)

---

### 4.4 Encryption & Security Placeholder

#### Password Security
- Passwords hashed using BCrypt

#### Transaction Integrity
- Store hash of transaction contents
- Optional digital signature field

Future:

- Public/private key simulation
- Signed transactions

#### API Security
- Auth token or session validation
- Protected endpoints for logged-in users only

---

### 4.5 Coin Supply / Price Logic Placeholder

Token price may depend on:

- Recent token demand
- Total supply
- Simple pricing formula

Example:

price = base_price + k * recent_demand

This supports:

- Price graph visualization
- Simulated market behavior

---

## 5. DevOps / Infrastructure

### 5.1 Docker Compose

Docker setup will start:

- Frontend container
- Backend container
- MySQL database container

Goal:

Run full stack with:

docker-compose up

---

### 5.2 CI Pipeline

On every push:

Frontend:
- Lint
- Build
- Test

Backend:
- Compile
- Run tests

---

### 5.3 Seed Data

Initial database seed includes:

- Sample users
- Sample wallets
- Sample products
- Sample transaction history
- Initial coin price history

---

## 6. Minimum Viable Features (MVP)

- User registration and login  
- Wallet balance display  
- Buy tokens  
- Spend tokens  
- Marketplace purchase  
- Transaction history  
- Basic coin price simulation  
- Working REST API with database  

---

## 7. Future Ideas (Optional)

- Token expiration over time  
- Transfer tokens between users  
- Blockchain-style block linking  
- Public/private key signing  
- Admin panel for economy control  
- More advanced price model  

---

## 8. Main Concept Summary

This project simulates a **time-based cryptocurrency economy** where time is treated as a scarce and tradable resource.

Tokens represent **minutes of privilege** and are used inside a controlled digital marketplace.

The system demonstrates:

- Cryptocurrency-style wallet mechanics
- Supply and demand pricing
- Secure transaction recording
- Full-stack web architecture
- Basic cryptographic concepts
