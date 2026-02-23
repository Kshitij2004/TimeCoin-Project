CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coins (
  symbol VARCHAR(10) PRIMARY KEY,
  current_price_usd DECIMAL(18, 2) NOT NULL,
  circulating_supply DECIMAL(18, 8) NOT NULL
);

CREATE TABLE IF NOT EXISTS wallets (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  symbol VARCHAR(10) NOT NULL,
  balance DECIMAL(18, 8) NOT NULL DEFAULT 0,
  UNIQUE KEY unique_wallet (user_id, symbol),
  CONSTRAINT fk_wallets_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT fk_wallets_coin
    FOREIGN KEY (symbol) REFERENCES coins(symbol)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS transactions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  symbol VARCHAR(10) NOT NULL,
  transaction_type ENUM('BUY', 'SELL') NOT NULL,
  quantity DECIMAL(18, 8) NOT NULL,
  price_usd DECIMAL(18, 2) NOT NULL,
  total_usd DECIMAL(18, 2) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_transactions_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT fk_transactions_coin
    FOREIGN KEY (symbol) REFERENCES coins(symbol)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

INSERT INTO users (id, email)
VALUES (1, 'demo@example.com')
ON DUPLICATE KEY UPDATE email = VALUES(email);

INSERT INTO coins (symbol, current_price_usd, circulating_supply)
VALUES ('BTC', 66000.00, 100.00000000)
ON DUPLICATE KEY UPDATE
  current_price_usd = VALUES(current_price_usd),
  circulating_supply = VALUES(circulating_supply);
