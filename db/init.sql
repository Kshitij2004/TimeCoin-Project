-- Create the database if it doesn't exist
CREATE DATABASE IF NOT EXISTS crypto_marketplace;
USE crypto_marketplace;

-- 1. Users Table
-- Stores user authentication and profile info.
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Wallets Table
-- Links specific coin balances to a user.
CREATE TABLE IF NOT EXISTS wallets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    coin_balance DECIMAL(18, 8) NOT NULL DEFAULT 0.00000000, -- 18 digits total, 8 decimal places for crypto precision
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Coins Table
-- Tracks global supply and current market details.
CREATE TABLE IF NOT EXISTS coins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    total_supply DECIMAL(20, 2) NOT NULL,
    circulating_supply DECIMAL(20, 2) NOT NULL,
    current_price DECIMAL(15, 2) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. Transactions Table
-- Ledger of all actions taken by users.
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    type ENUM('BUY', 'SELL', 'DEPOSIT', 'WITHDRAWAL') NOT NULL,
    amount DECIMAL(18, 8) NOT NULL,
    price_at_time DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);
