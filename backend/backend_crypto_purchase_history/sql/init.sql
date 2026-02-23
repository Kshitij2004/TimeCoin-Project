CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    ON UPDATE CASCADE
);

INSERT INTO users (id, email)
VALUES (1, 'demo@example.com')
ON DUPLICATE KEY UPDATE email = VALUES(email);

INSERT INTO transactions (user_id, symbol, transaction_type, quantity, price_usd, total_usd, created_at)
VALUES
  (1, 'TC', 'BUY', 1.50000000, 65000.00, 97500.00, '2026-02-23 10:00:00'),
  (1, 'TC', 'SELL', 0.50000000, 65500.00, 32750.00, '2026-02-23 11:00:00'),
  (1, 'TC', 'BUY', 0.25000000, 66000.00, 16500.00, '2026-02-23 12:00:00');
