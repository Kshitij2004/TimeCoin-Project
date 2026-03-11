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
-- Links specific coin balances to a user. Extended with blockchain identity fields.
CREATE TABLE IF NOT EXISTS wallets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    wallet_address VARCHAR(128) NOT NULL UNIQUE,
    public_key VARCHAR(512) NOT NULL UNIQUE,
    coin_balance DECIMAL(18, 8) NOT NULL DEFAULT 0.00000000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_wallets_address ON wallets(wallet_address);

-- 3. Coins Table
-- Tracks global supply and current market details.
CREATE TABLE IF NOT EXISTS coins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    total_supply DECIMAL(20, 2) NOT NULL,
    circulating_supply DECIMAL(20, 2) NOT NULL,
    current_price DECIMAL(15, 2) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. Blocks Table
-- Each block links to the previous block, forming the chain.
CREATE TABLE IF NOT EXISTS blocks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    block_height INT NOT NULL UNIQUE,
    previous_hash VARCHAR(128),
    block_hash VARCHAR(128) NOT NULL UNIQUE,
    validator_address VARCHAR(128),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    transaction_count INT NOT NULL DEFAULT 0,
    status ENUM('PENDING', 'COMMITTED', 'INVALID') NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_blocks_height ON blocks(block_height);
CREATE INDEX idx_blocks_hash ON blocks(block_hash);
CREATE INDEX idx_blocks_validator ON blocks(validator_address);

-- 5. Transactions Table
-- Blockchain-style transactions with sender/receiver addresses, hash, and block linkage.
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_address VARCHAR(128),
    receiver_address VARCHAR(128) NOT NULL,
    amount DECIMAL(18, 8) NOT NULL,
    fee DECIMAL(18, 8) NOT NULL DEFAULT 0.00000000,
    nonce INT NOT NULL DEFAULT 0,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    transaction_hash VARCHAR(128) NOT NULL UNIQUE,
    status ENUM('PENDING', 'CONFIRMED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    block_id INT DEFAULT NULL,
    FOREIGN KEY (block_id) REFERENCES blocks(id) ON DELETE SET NULL
);

CREATE INDEX idx_tx_sender ON transactions(sender_address);
CREATE INDEX idx_tx_receiver ON transactions(receiver_address);
CREATE INDEX idx_tx_status ON transactions(status);
CREATE INDEX idx_tx_hash ON transactions(transaction_hash);
CREATE INDEX idx_tx_block ON transactions(block_id);

-- 6. Block Transactions Table
-- Join table linking blocks to transactions for easy querying.
CREATE TABLE IF NOT EXISTS block_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    block_id INT NOT NULL,
    transaction_id INT NOT NULL,
    FOREIGN KEY (block_id) REFERENCES blocks(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_block_tx (block_id, transaction_id)
);

-- 7. Validators Table
-- Tracks users who have registered as validators by staking TimeCoin.
CREATE TABLE IF NOT EXISTS validators (
    id INT AUTO_INCREMENT PRIMARY KEY,
    wallet_address VARCHAR(128) NOT NULL UNIQUE,
    staked_amount DECIMAL(18, 8) NOT NULL DEFAULT 0.00000000,
    status ENUM('ACTIVE', 'INACTIVE', 'JAILED', 'UNSTAKING') NOT NULL DEFAULT 'INACTIVE',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_selected_at TIMESTAMP NULL,
    FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) ON DELETE CASCADE
);

CREATE INDEX idx_validators_status ON validators(status);
CREATE INDEX idx_validators_address ON validators(wallet_address);

-- 8. Staking Events Table
-- Records of stake and unstake actions for audit trail.
CREATE TABLE IF NOT EXISTS staking_events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    wallet_address VARCHAR(128) NOT NULL,
    event_type ENUM('STAKE', 'UNSTAKE') NOT NULL,
    amount DECIMAL(18, 8) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) ON DELETE CASCADE
);

CREATE INDEX idx_staking_address ON staking_events(wallet_address);

-- 9. Listings Table
-- Marketplace listings where users sell goods/services for TimeCoin.
CREATE TABLE IF NOT EXISTS listings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(18, 8) NOT NULL,
    category VARCHAR(100),
    status ENUM('ACTIVE', 'SOLD', 'REMOVED') NOT NULL DEFAULT 'ACTIVE',
    image_url VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_listings_seller ON listings(seller_id);
CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_category ON listings(category);