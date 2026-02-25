package com.cs506.cryptopurchase.repository;

import com.cs506.cryptopurchase.model.CoinSnapshot;
import com.cs506.cryptopurchase.model.TransactionSnapshot;
import com.cs506.cryptopurchase.model.WalletSnapshot;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPurchaseRepository implements PurchaseRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPurchaseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean userExists(int userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE id = ?",
            Integer.class,
            userId
        );
        return count != null && count > 0;
    }

    @Override
    public Optional<CoinSnapshot> lockCoin(String symbol) {
        List<CoinSnapshot> rows = jdbcTemplate.query(
            "SELECT symbol, current_price_usd, circulating_supply FROM coins WHERE symbol = ? FOR UPDATE",
            (rs, rowNum) -> new CoinSnapshot(
                rs.getString("symbol"),
                rs.getBigDecimal("current_price_usd"),
                rs.getBigDecimal("circulating_supply")
            ),
            symbol
        );
        return rows.stream().findFirst();
    }

    @Override
    public void decrementSupply(String symbol, BigDecimal amount) {
        jdbcTemplate.update(
            "UPDATE coins SET circulating_supply = circulating_supply - ? WHERE symbol = ?",
            amount,
            symbol
        );
    }

    @Override
    public void upsertWallet(int userId, String symbol, BigDecimal amount) {
        jdbcTemplate.update(
            """
            INSERT INTO wallets (user_id, symbol, balance)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)
            """,
            userId,
            symbol,
            amount
        );
    }

    @Override
    public Optional<WalletSnapshot> findWallet(int userId, String symbol) {
        List<WalletSnapshot> rows = jdbcTemplate.query(
            "SELECT user_id, symbol, balance FROM wallets WHERE user_id = ? AND symbol = ?",
            (rs, rowNum) -> new WalletSnapshot(
                rs.getInt("user_id"),
                rs.getString("symbol"),
                rs.getBigDecimal("balance")
            ),
            userId,
            symbol
        );
        return rows.stream().findFirst();
    }

    @Override
    public long insertTransaction(int userId, String symbol, BigDecimal amount, BigDecimal priceUsd, BigDecimal totalUsd) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO transactions
                (user_id, symbol, transaction_type, quantity, price_usd, total_usd)
                VALUES (?, ?, 'BUY', ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, userId);
            ps.setString(2, symbol);
            ps.setBigDecimal(3, amount);
            ps.setBigDecimal(4, priceUsd);
            ps.setBigDecimal(5, totalUsd);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? -1L : key.longValue();
    }

    @Override
    public Optional<TransactionSnapshot> findTransaction(long id) {
        List<TransactionSnapshot> rows = jdbcTemplate.query(
            """
            SELECT id, user_id, symbol, transaction_type, quantity, price_usd, total_usd, created_at
            FROM transactions
            WHERE id = ?
            """,
            (rs, rowNum) -> new TransactionSnapshot(
                rs.getLong("id"),
                rs.getInt("user_id"),
                rs.getString("symbol"),
                rs.getString("transaction_type"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("price_usd"),
                rs.getBigDecimal("total_usd"),
                rs.getTimestamp("created_at").toLocalDateTime()
            ),
            id
        );
        return rows.stream().findFirst();
    }
}
