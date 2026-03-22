-- Seed data for MySQL database

USE crypto_marketplace;

-- Coin state
INSERT INTO coins (id, total_supply, circulating_supply, current_price, updated_at)
VALUES
    (1, 1000000.00, 500000.00, 10.00, NOW());

-- Users with email, pass, id, etc
INSERT INTO users (id, username, email, password_hash, created_at)
VALUES
    (1,'dominckweston', 'dsweston@wisc.edu', '$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW()),
    (2,'andrewmcdonagh', 'almcdonagh@wisc.edu', '$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW()),
    (3,'kshitijpandey', 'kpandey4@wisc.edu', '$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW()),
    (4,'garvpundir', 'gpundir@wisc.edu', '$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW()),
    (5,'tianzegao', 'tgao67@wisc.edu', 'f$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW()),
    (6,'jeremiahjin', 'zjin254@wisc.edu', '$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW());

-- Wallets for users
INSERT INTO wallets (id, user_id, wallet_address, public_key, coin_balance, created_at)
VALUES
    (1, 1, 'addr_1', 'pubkey_1', 0.00000000, NOW()),
    (2, 2, 'addr_2', 'pubkey_2', 0.00000000, NOW()),
    (3, 3, 'addr_3', 'pubkey_3', 0.00000000, NOW()),
    (4, 4, 'addr_4', 'pubkey_4', 0.00000000, NOW()),
    (5, 5, 'addr_5', 'pubkey_5', 0.00000000, NOW()),
    (6, 6, 'addr_6', 'pubkey_6', 0.00000000, NOW());


-- Transaction data, need to change init.sql to reflect a transfer from account to account or if it goes into an intermediary acct. etc
INSERT INTO transactions (id, sender_address, receiver_address, amount, user_id, symbol, transaction_type, price_at_time, total_usd, fee, nonce, timestamp, transaction_hash, status, block_id)
VALUES
-- Minting for accounts
    (1, NULL, 'addr_1', 50.000000, NULL , 'TC', 'DEPOSIT', 10.000000, 50.000000, 0.01, 0, CURRENT_TIMESTAMP, 'tx_hash_001', 'CONFIRMED', 1),
    (2, NULL, 'addr_2', 1.000000, NULL , 'TC', 'DEPOSIT', 10.000000, 10.000000, 0.01, 0, CURRENT_TIMESTAMP, 'tx_hash_002', 'CONFIRMED', 1),
    (3, NULL, 'addr_3', 35.000000, NULL , 'TC', 'DEPOSIT', 10.000000, 350.000000, 0.01, 0, CURRENT_TIMESTAMP, 'tx_hash_003', 'CONFIRMED', 1),
    (4, NULL, 'addr_4', 100.000000, NULL , 'TC', 'DEPOSIT', 10.000000, 1000.000000, 0.01, 0, CURRENT_TIMESTAMP, 'tx_hash_004', 'CONFIRMED', 1),
    (5, NULL, 'addr_5', 1000.000000, NULL , 'TC', 'DEPOSIT', 10.000000, 10000.000000, 0.01, 0, CURRENT_TIMESTAMP, 'tx_hash_005', 'CONFIRMED', 1),
    (6, NULL, 'addr_6', 62.000000, NULL , 'TC', 'DEPOSIT', 10.000000, 620.000000, 0.01, 0, CURRENT_TIMESTAMP, 'tx_hash_006', 'CONFIRMED', 1),
-- Confirmed/working transaction
    (7, 'addr_5', 'addr_3', 5.000000, 1 , 'TC', 'BUY', 10.000000, 50.00, 0.01, 1, CURRENT_TIMESTAMP, 'tx_hash_007', 'CONFIRMED', 2),

-- Pending transaction
    (8, 'addr_5', 'addr_6', 10.000000, 5 , 'TC', 'BUY', 10.000000, 100.000000, 0.01, 1, CURRENT_TIMESTAMP, 'tx_hash_008', 'PENDING', 3),

-- Failed/insufficient balance transaction
(8, 'addr_1', 'addr_2', 10.000000, 1 , 'TC', 'BUY', 10.000000, 100.000000, 0.01, 1, CURRENT_TIMESTAMP, 'tx_hash_008', 'REJECTED', 3);


-- Listing table
INSERT INTO listings (id, seller_id, title, description, price, category, status, image_url, created_at)
VALUES
    (1, 1, 'DUMMY TRANSACTION', 'Listing seed data.', 5.000000, 'DUMMY', 'ACTIVE', NULL, CURRENT_TIMESTAMP);