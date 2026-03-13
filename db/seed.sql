-- Seed data for MySQL database

USE crypto_marketplace;

-- Coin state
INSERT INTO coins (total_supply, circulating_supply, current_price)
VALUES
    (1000000.00, 500000.00, 10.00);

-- Users with email, pass, id, etc
INSERT INTO users (id, username, email, password_hash, created_at)
VALUES
    (1,'testuser1', 'test1@wisc.edu', 'fakehash', NOW()),
    (2,'testuser2', 'test2@wisc.edu', 'fakehash', NOW()),
    (3,'testuser3', 'test3@wisc.edu', 'fakehash', NOW()),
    (4,'testuser4', 'test4@wisc.edu', 'fakehash', NOW()),
    (5,'testuser5', 'test5@wisc.edu', 'fakehash', NOW()),
    (6,'testuser6', 'test6@wisc.edu', 'fakehash', NOW()),
    (7,'testuser7', 'test7@wisc.edu', 'fakehash', NOW()),
    (8,'testuser8', 'test8@wisc.edu', 'fakehash', NOW());

-- Wallets for users
INSERT INTO wallets (id, user_id, wallet_address, public_key, coin_balance, created_at)
VALUES
    (1, 1, 'wlt_seed_1', 'pub_seed_1', 100.00000000, NOW()),
    (2, 2, 'wlt_seed_2', 'pub_seed_2', 5000.00000000, NOW()),
    (3, 3, 'wlt_seed_3', 'pub_seed_3', 300.00000000, NOW()),
    (4, 4, 'wlt_seed_4', 'pub_seed_4', 10.00000000, NOW()),
    (5, 5, 'wlt_seed_5', 'pub_seed_5', 40.00000000, NOW()),
    (6, 6, 'wlt_seed_6', 'pub_seed_6', 90000.00000000, NOW()),
    (7, 7, 'wlt_seed_7', 'pub_seed_7', 490.00000000, NOW()),
    (8, 8, 'wlt_seed_8', 'pub_seed_8', 156.00000000, NOW());

-- Sample purchase history
INSERT INTO transactions (
    id,
    sender_address,
    user_id,
    symbol,
    receiver_address,
    amount,
    transaction_type,
    price_at_time,
    total_usd,
    fee,
    nonce,
    timestamp,
    transaction_hash,
    status,
    block_id
)
VALUES
    (1, NULL, 1, 'TC', 'wlt_seed_1', 1.50000000, 'BUY', 10.00, 15.00, 0.00000000, 0, NOW() - INTERVAL 3 DAY, 'seed_tx_1', 'CONFIRMED', NULL),
    (2, NULL, 1, 'TC', 'wlt_seed_1', 0.50000000, 'SELL', 11.00, 5.50, 0.00000000, 0, NOW() - INTERVAL 2 DAY, 'seed_tx_2', 'CONFIRMED', NULL),
    (3, NULL, 1, 'TC', 'wlt_seed_1', 2.00000000, 'BUY', 12.00, 24.00, 0.00000000, 0, NOW() - INTERVAL 1 DAY, 'seed_tx_3', 'CONFIRMED', NULL);
