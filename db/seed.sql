-- Seed data for MySQL database

USE crypto_marketplace;

-- Coin state
INSERT INTO coins (total_supply, circulating_supply, current_price)
VALUES
    (1000000.00, 500000.00, 10.00);

-- Users with email, pass, id, etc
INSERT INTO users (id, username, email, password_hash, created_at)
VALUES
    (user1,'testuser1', 'test1@wisc.edu', 'fakehash', NOW()),
    (user2,'testuser2', 'test2@wisc.edu', 'fakehash', NOW()),
    (user3,'testuser3', 'test3@wisc.edu', 'fakehash', NOW()),
    (user4,'testuser4', 'test4@wisc.edu', 'fakehash', NOW()),
    (user5,'testuser5', 'test5@wisc.edu', 'fakehash', NOW()),
    (user6,'testuser6', 'test6@wisc.edu', 'fakehash', NOW()),
    (user7,'testuser7', 'test7@wisc.edu', 'fakehash', NOW()),
    (user8,'testuser8', 'test8@wisc.edu', 'fakehash', NOW());

-- Wallets for users
INSERT INTO wallets (id, user_id, coin_balance, created_at)
VALUES
    (wallet1, user1, 100.00000000, NOW()),
    (wallet2, user2, 5000.00000000, NOW()),
    (wallet3, user3, 300.00000000, NOW()),
    (wallet4, user4, 10.00000000, NOW()),
    (wallet5, user5, 40.00000000, NOW()),
    (wallet6, user6, 90000.00000000, NOW()),
    (wallet7, user7, 490.00000000, NOW()),
    (wallet8, user8, 156.00000000, NOW());

-- Transaction data, need to change init.sql to reflect a transfer from account to account or if it goes into an intermediary acct. etc
INSERT INTO transactions (id, user_id, type, amount, price_at_time, created_at)
VALUES
-- User to user transactions (from_user, to_user)
--     (1, 7, 'TRANSFER', 50, NOW() - INTERVAL 5 DAY),
--     (2, 8, 'TRANSFER', 1000, NOW() - INTERVAL 4 DAY),
--     (3, 4, 'TRANSFER', 20, NOW() - INTERVAL 3 DAY),

-- User from market
    (7, 8, 'BUY', 150, NOW() - INTERVAL 2 DAY, NOW()),
    (8, 9, 'SELL', 250, NOW() - INTERVAL 1 DAY, NOW()),

-- Deposit/withdraw example
    (NULL, 7, 'DEPOSIT', 50, NOW(), NOW()),
    (NULL, 7, 'WITHDRAWAL', 50,  NOW(), NOW());