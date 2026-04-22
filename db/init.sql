CREATE DATABASE IF NOT EXISTS crypto_marketplace;
USE crypto_marketplace;

-- 1. Users
-- foundation for auth; all other user-facing entities reference this
CREATE TABLE IF NOT EXISTS users (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,    -- bcrypt output, never plain text
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    two_factor_secret VARCHAR(64) NULL,     -- Base32 TOTP secret; null until 2FA is set up
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE
    );

-- 2. Wallets
-- wallet_address is derived from public_key and serves as the on-chain identity
-- used in transactions. keeping both allows signature verification without re-deriving.
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
-- single-row table for global TimeCoin supply/price metadata so the frontend
-- doesn't have to compute supply from the entire transaction history
CREATE TABLE IF NOT EXISTS coins (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     total_supply DECIMAL(20, 2) NOT NULL,
    circulating_supply DECIMAL(20, 2) NOT NULL,
    current_price DECIMAL(15, 2) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

-- 4. Blocks
-- core of the blockchain ledger. each block references its predecessor's hash
-- to form the chain. block_height unique constraint prevents forks at the DB level.
CREATE TABLE IF NOT EXISTS blocks (
                                      id INT AUTO_INCREMENT PRIMARY KEY,
                                      block_height INT NOT NULL UNIQUE,
                                      previous_hash VARCHAR(128),                  -- null only for genesis block
    block_hash VARCHAR(128) NOT NULL UNIQUE,
    validator_address VARCHAR(128),               -- null for genesis
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    transaction_count INT NOT NULL DEFAULT 0,     -- denormalized for fast block-explorer queries
    status ENUM('PENDING', 'COMMITTED', 'INVALID') NOT NULL DEFAULT 'PENDING'
    );

CREATE INDEX idx_blocks_height ON blocks(block_height);
CREATE INDEX idx_blocks_hash ON blocks(block_hash);
CREATE INDEX idx_blocks_validator ON blocks(validator_address);

-- 5. Transactions
-- uses wallet addresses (not user IDs) because on-chain identity is the address.
-- sender_address is nullable for BUY/coinbase transactions (coins drawn from supply).
-- receiver_address is nullable for SELL transactions (coins returned to supply).
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_address VARCHAR(128),                                -- null for BUY/coinbase
    receiver_address VARCHAR(128),                              -- null for SELL (returned to supply)
    amount DECIMAL(18, 8) NOT NULL,
    user_id INT DEFAULT NULL,                                   -- set for user-facing buy/sell history rows
    symbol VARCHAR(10) DEFAULT NULL,
    transaction_type ENUM('BUY', 'SELL', 'TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'MINT') DEFAULT NULL,
    price_at_time DECIMAL(15, 2) DEFAULT NULL,
    total_usd DECIMAL(18, 2) DEFAULT NULL,
    fee DECIMAL(18, 8) NOT NULL DEFAULT 0.00000000,
    nonce INT NOT NULL DEFAULT 0,                                -- per-sender sequence number to prevent replay attacks
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    transaction_hash VARCHAR(128) NOT NULL UNIQUE,               -- SHA-256 over canonical fields; deterministic and tamper-evident
    status ENUM('PENDING', 'CONFIRMED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    block_id INT DEFAULT NULL,                                   -- null while in mempool, set once included in a block
    FOREIGN KEY (block_id) REFERENCES blocks(id) ON DELETE SET NULL  -- SET NULL so tx history survives block reorgs
    );

-- heavily indexed because this is the most-queried table (balance computation,
-- mempool filtering, block assembly all hit it). Per-sender nonce sequencing is
-- enforced at the service layer in TransactionValidationService rather than
-- at the DB level, since buy/sell transactions all use nonce=0 by design.
CREATE INDEX idx_tx_sender ON transactions(sender_address);
CREATE INDEX idx_tx_receiver ON transactions(receiver_address);
CREATE INDEX idx_tx_status ON transactions(status);
CREATE INDEX idx_tx_hash ON transactions(transaction_hash);
CREATE INDEX idx_tx_block ON transactions(block_id);
CREATE INDEX idx_tx_user_type_time ON transactions(user_id, transaction_type, timestamp);

-- 6. Block-Transaction Join Table
-- exists alongside transactions.block_id for efficient "get all txs in block X"
-- queries without scanning the full transactions table
CREATE TABLE IF NOT EXISTS block_transactions (
                                                  id INT AUTO_INCREMENT PRIMARY KEY,
                                                  block_id INT NOT NULL,
                                                  transaction_id INT NOT NULL,
                                                  FOREIGN KEY (block_id) REFERENCES blocks(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_block_tx (block_id, transaction_id)
    );

-- 7. Validators
-- users who opt into PoS consensus by staking TimeCoin.
-- references wallet_address (not user_id) because validator identity is on-chain.
CREATE TABLE IF NOT EXISTS validators (
                                          id INT AUTO_INCREMENT PRIMARY KEY,
                                          wallet_address VARCHAR(128) NOT NULL UNIQUE,
    staked_amount DECIMAL(18, 8) NOT NULL DEFAULT 0.00000000,
    status ENUM('ACTIVE', 'INACTIVE', 'JAILED', 'UNSTAKING') NOT NULL DEFAULT 'INACTIVE',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_selected_at TIMESTAMP NULL,             -- tracks recency to prevent the same validator from dominating
    FOREIGN KEY (wallet_address) REFERENCES wallets(wallet_address) ON DELETE CASCADE
    );

CREATE INDEX idx_validators_status ON validators(status);
CREATE INDEX idx_validators_address ON validators(wallet_address);

-- 8. Staking Events
-- immutable audit log of every stake/unstake action, separate from validators
-- so current staked_amount can be independently verified by summing events
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
-- marketplace layer where users post goods/services priced in TimeCoin.
-- references seller_id (user-level) because marketplace identity is the account, not a wallet.
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
-- stores active refresh tokens issued at login. deleted on use (rotation)
-- or on logout. expires_at is checked server-side before issuing a new access token.
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
-- tracks coin price over time for charting. one row per recalculation.
CREATE TABLE IF NOT EXISTS price_history (
                                             id INT AUTO_INCREMENT PRIMARY KEY,
                                             price DECIMAL(15, 2) NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX idx_price_history_time ON price_history(recorded_at);

-- 12. Mining Accumulator
-- tracks click-based mining progress for each wallet. window_start and last_mined_at
-- allow enforcing cooldowns and time-based limits on mining rewards.
CREATE TABLE IF NOT EXISTS mining_accumulator (
                                                  wallet_address  VARCHAR(128)  NOT NULL PRIMARY KEY,
    click_count     INT           NOT NULL DEFAULT 0,
    window_start    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_mined_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
