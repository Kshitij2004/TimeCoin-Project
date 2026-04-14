USE crypto_marketplace;

-- Coin state
INSERT IGNORE INTO coins (id, total_supply, circulating_supply, current_price, updated_at)
VALUES (1, 1000000.00, 500000.00, 10.00, NOW());

-- Agent bot accounts (team member names, used as trading agents)
INSERT INTO users (id, username, email, password_hash, created_at)
VALUES
    (1, 'dominckweston',  'dsweston@wisc.edu',   '$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW()),
    (2, 'andrewmcdonagh', 'almcdonagh@wisc.edu', '$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW()),
    (3, 'kshitijpandey',  'kpandey4@wisc.edu',   '$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW()),
    (4, 'garvpundir',     'gpundir@wisc.edu',    '$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW()),
    (5, 'tianzegao',      'tgao67@wisc.edu',     '$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW()),
    (6, 'jeremiahjin',    'zjin254@wisc.edu',    '$2a$10$.bEjHwoFDwZl2kjqJReSg.rX9QiARO9QB0w5p1JLXjeUnrXnSwpty', NOW());

-- Agent wallets
INSERT INTO wallets (id, user_id, wallet_address, public_key, coin_balance, created_at)
VALUES
    (1, 1, 'addr_1', 'pubkey_1', 0.00000000, NOW()),
    (2, 2, 'addr_2', 'pubkey_2', 0.00000000, NOW()),
    (3, 3, 'addr_3', 'pubkey_3', 0.00000000, NOW()),
    (4, 4, 'addr_4', 'pubkey_4', 0.00000000, NOW()),
    (5, 5, 'addr_5', 'pubkey_5', 0.00000000, NOW()),
    (6, 6, 'addr_6', 'pubkey_6', 0.00000000, NOW());

-- Agent minting (CONFIRMED so agents have spendable balance)
INSERT INTO transactions (id, sender_address, receiver_address, amount, user_id, symbol, transaction_type, price_at_time, total_usd, fee, nonce, timestamp, transaction_hash, status, block_id)
VALUES
    (1, NULL, 'addr_1', 5000.00000000, NULL, 'TC', 'DEPOSIT', 10.00, 50000.00, 0.00000000, 0, NOW(), 'agent_mint_1', 'CONFIRMED', NULL),
    (2, NULL, 'addr_2', 5000.00000000, NULL, 'TC', 'DEPOSIT', 10.00, 50000.00, 0.00000000, 0, NOW(), 'agent_mint_2', 'CONFIRMED', NULL),
    (3, NULL, 'addr_3', 5000.00000000, NULL, 'TC', 'DEPOSIT', 10.00, 50000.00, 0.00000000, 0, NOW(), 'agent_mint_3', 'CONFIRMED', NULL),
    (4, NULL, 'addr_4', 5000.00000000, NULL, 'TC', 'DEPOSIT', 10.00, 50000.00, 0.00000000, 0, NOW(), 'agent_mint_4', 'CONFIRMED', NULL),
    (5, NULL, 'addr_5', 5000.00000000, NULL, 'TC', 'DEPOSIT', 10.00, 50000.00, 0.00000000, 0, NOW(), 'agent_mint_5', 'CONFIRMED', NULL),
    (6, NULL, 'addr_6', 5000.00000000, NULL, 'TC', 'DEPOSIT', 10.00, 50000.00, 0.00000000, 0, NOW(), 'agent_mint_6', 'CONFIRMED', NULL);