package com.cs506.cryptohistory.repository;

import com.cs506.cryptohistory.model.TransactionRow;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTransactionHistoryRepository implements TransactionHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTransactionHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int countByUserId(int userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM transactions WHERE user_id = ?",
            Integer.class,
            userId
        );
        return count == null ? 0 : count;
    }

    @Override
    public List<TransactionRow> findByUserId(int userId, int limit, int offset) {
        return jdbcTemplate.query(
            """
            SELECT transaction_type, quantity, price_usd, created_at
            FROM transactions
            WHERE user_id = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> new TransactionRow(
                rs.getString("transaction_type"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("price_usd"),
                rs.getTimestamp("created_at").toLocalDateTime()
            ),
            userId,
            limit,
            offset
        );
    }
}
