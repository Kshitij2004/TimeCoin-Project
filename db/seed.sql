INSERT INTO coins (total_supply, circulating_supply, current_price)
VALUES (1000000.00, 500000.00, 10.00);

INSERT INTO users (username, email, password_hash)
VALUES ('testuser', 'test@wisc.edu', 'fakehash');

INSERT INTO wallets (user_id, coin_balance)
VALUES (1, 5.00000000);