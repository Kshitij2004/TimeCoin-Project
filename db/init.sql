CREATE DATABASE IF NOT EXISTS crypto_marketplace;
USE crypto_marketplace;

-- 1. Users
CREATE TABLE IF NOT EXISTS users (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    two_factor_secret VARCHAR(64) NULL,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE
    );

-- 2. Wallets
CREATE TABLE IF NOT EXISTS wallets (
                                       id INT AUTO_INCREMENT PRIMARY KEY,
                                       user_id INT NOT NULL UNIQUE,
                                       wallet_address VARCHAR(128) NOT NULL UNIQUE,
    public_key VARCHAR(512) NOT NULL UNIQUE,
    coin_balance DECIMAL(18, 8) NOT NULL DEFAULT 0.00000000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_wallets_address ON wallets(wallet_address);

-- 3. Coins
CREATE TABLE IF NOT EXISTS coins (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     total_supply DECIMAL(20, 2) NOT NULL,
    circulating_supply DECIMAL(20, 2) NOT NULL,
    current_price DECIMAL(15, 2) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

-- 4. Blocks
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

-- 5. Transactions
-- sender_address is nullable for BUY/coinbase transactions (coins drawn from supply).
-- receiver_address is nullable for SELL transactions (coins returned to supply).
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_address VARCHAR(128),                                -- null for BUY/coinbase
    receiver_address VARCHAR(128),                              -- null for SELL (returned to supply)
    amount DECIMAL(18, 8) NOT NULL,
    user_id INT DEFAULT NULL,
    symbol VARCHAR(10) DEFAULT NULL,
    transaction_type ENUM('BUY', 'SELL', 'TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'MINT') DEFAULT NULL,
    price_at_time DECIMAL(15, 2) DEFAULT NULL,
    total_usd DECIMAL(18, 2) DEFAULT NULL,
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
CREATE INDEX idx_tx_user_type_time ON transactions(user_id, transaction_type, timestamp);

-- 6. Block-Transaction Join Table
CREATE TABLE IF NOT EXISTS block_transactions (
                                                  id INT AUTO_INCREMENT PRIMARY KEY,
                                                  block_id INT NOT NULL,
                                                  transaction_id INT NOT NULL,
                                                  FOREIGN KEY (block_id) REFERENCES blocks(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_block_tx (block_id, transaction_id)
    );

-- 7. Validators
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

-- 8. Staking Events
CREATE TABLE IF NOT EXISTS staking_events (
                                              id INT AUTO_INCREMENT PRIMARY KEY,
                                              wallet_address VARCHAR(128) NOT NULL,
    event_type ENUM('STAKE', 'UNSTAKE') NOT NULL,
    amount DECIMAL(18, 8) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) ON DELETE CASCADE
    );

CREATE INDEX idx_staking_address ON staking_events(wallet_address);

-- 9. Listings
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

-- 10. Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id INT AUTO_INCREMENT PRIMARY KEY,
                                              token VARCHAR(512) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- 11. Price History
CREATE TABLE IF NOT EXISTS price_history (
                                             id INT AUTO_INCREMENT PRIMARY KEY,
                                             price DECIMAL(15, 2) NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX idx_price_history_time ON price_history(recorded_at);

-- 12. Mining Accumulator
CREATE TABLE IF NOT EXISTS mining_accumulator (
                                                  wallet_address  VARCHAR(128)  NOT NULL PRIMARY KEY,
    click_count     INT           NOT NULL DEFAULT 0,
    window_start    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_mined_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
