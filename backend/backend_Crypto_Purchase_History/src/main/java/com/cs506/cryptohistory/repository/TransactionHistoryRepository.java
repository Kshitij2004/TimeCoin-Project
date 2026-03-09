package com.cs506.cryptohistory.repository;

import com.cs506.cryptohistory.model.TransactionRow;
import java.util.List;

public interface TransactionHistoryRepository {
    int countByUserId(int userId);
    List<TransactionRow> findByUserId(int userId, int limit, int offset);
}
