package t_12.backend.integration;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import t_12.backend.entity.Block;
import t_12.backend.entity.BlockTransaction;
import t_12.backend.entity.Transaction;
import t_12.backend.repository.BlockRepository;
import t_12.backend.repository.BlockTransactionRepository;
import t_12.backend.repository.TransactionRepository;

/**
 * End-to-end API tests for blockchain explorer endpoints and response contracts.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:blockchain_explorer_api_test_db;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class BlockchainExplorerApiIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BlockTransactionRepository blockTransactionRepository;

    @BeforeEach
    void cleanTables() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        blockTransactionRepository.deleteAll();
        transactionRepository.deleteAll();
        blockRepository.deleteAll();
    }

    @Test
    void statusEndpointReturnsCurrentChainSummaryWithoutAuth() throws Exception {
        Block block0 = saveBlock(0, "hash0", "000", Block.Status.COMMITTED, LocalDateTime.of(2026, 3, 1, 10, 0));
        saveBlock(1, "hash1", block0.getBlockHash(), Block.Status.COMMITTED, LocalDateTime.of(2026, 3, 1, 10, 1));
        saveBlock(2, "hash2", "hash1", Block.Status.PENDING, LocalDateTime.of(2026, 3, 1, 10, 2));

        saveTransaction("pending_tx_1", null, Transaction.Status.PENDING, 10);
        saveTransaction("confirmed_tx_1", block0.getId(), Transaction.Status.CONFIRMED, 11);

        mockMvc.perform(get("/api/chain/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBlocks").value(3))
                .andExpect(jsonPath("$.committedBlocks").value(2))
                .andExpect(jsonPath("$.pendingTransactions").value(1))
                .andExpect(jsonPath("$.latestBlockHeight").value(2))
                .andExpect(jsonPath("$.latestBlockHash").value("hash2"));
    }

    @Test
    void blocksEndpointReturnsPaginatedNewestFirst() throws Exception {
        saveBlock(0, "hash0", "000", Block.Status.COMMITTED, LocalDateTime.of(2026, 3, 1, 10, 0));
        saveBlock(1, "hash1", "hash0", Block.Status.COMMITTED, LocalDateTime.of(2026, 3, 1, 10, 1));
        saveBlock(2, "hash2", "hash1", Block.Status.COMMITTED, LocalDateTime.of(2026, 3, 1, 10, 2));
        saveBlock(3, "hash3", "hash2", Block.Status.COMMITTED, LocalDateTime.of(2026, 3, 1, 10, 3));
        saveBlock(4, "hash4", "hash3", Block.Status.COMMITTED, LocalDateTime.of(2026, 3, 1, 10, 4));

        mockMvc.perform(get("/api/chain/blocks")
                        .param("page", "2")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(2))
                .andExpect(jsonPath("$.pagination.limit").value(2))
                .andExpect(jsonPath("$.pagination.total").value(5))
                .andExpect(jsonPath("$.pagination.totalPages").value(3))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].blockHeight").value(2))
                .andExpect(jsonPath("$.data[1].blockHeight").value(1));
    }

    @Test
    void blockDetailEndpointIncludesLinkedTransactions() throws Exception {
        Block block = saveBlock(7, "hash7", "hash6", Block.Status.COMMITTED, LocalDateTime.of(2026, 3, 1, 11, 0));

        Transaction tx1 = saveTransaction("tx_hash_1", block.getId(), Transaction.Status.CONFIRMED, 21);
        Transaction tx2 = saveTransaction("tx_hash_2", block.getId(), Transaction.Status.CONFIRMED, 22);
        saveBlockTransaction(block.getId(), tx1.getId());
        saveBlockTransaction(block.getId(), tx2.getId());

        mockMvc.perform(get("/api/chain/blocks/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockHeight").value(7))
                .andExpect(jsonPath("$.blockHash").value("hash7"))
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.transactions[*].transactionHash", hasItems("tx_hash_1", "tx_hash_2")));
    }

    @Test
    void blockByHashEndpointReturnsMatchingBlockDetail() throws Exception {
        Block block = saveBlock(9, "hash9", "hash8", Block.Status.COMMITTED, LocalDateTime.of(2026, 3, 1, 11, 15));
        Transaction tx = saveTransaction("tx_hash_9", block.getId(), Transaction.Status.CONFIRMED, 31);
        saveBlockTransaction(block.getId(), tx.getId());

        mockMvc.perform(get("/api/chain/blocks/hash/hash9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockHeight").value(9))
                .andExpect(jsonPath("$.blockHash").value("hash9"))
                .andExpect(jsonPath("$.transactions.length()").value(1))
                .andExpect(jsonPath("$.transactions[0].transactionHash").value("tx_hash_9"));
    }

    @Test
    void blockByHashEndpointReturnsNotFoundForUnknownHash() throws Exception {
        mockMvc.perform(get("/api/chain/blocks/hash/missing_hash"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void blocksEndpointRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/chain/blocks")
                        .param("page", "0")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page must be a positive integer"));
    }

    @Test
    void minePendingEndpointCommitsPendingTransactionsIntoNewBlock() throws Exception {
        saveBlock(0, "genesis_hash", "000", Block.Status.COMMITTED, LocalDateTime.of(2026, 3, 1, 9, 0));
        saveTransaction("tx_pending_1", null, Transaction.Status.PENDING, 77);

        String token = Jwts.builder()
                .subject("1")
                .signWith(Keys.hmacShaKeyFor("your-super-secret-key-change-this-in-production".getBytes()))
                .compact();

        mockMvc.perform(post("/api/chain/mine-pending")
                        .param("limit", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blockHeight").value(1))
                .andExpect(jsonPath("$.transactions.length()").value(1))
                .andExpect(jsonPath("$.transactions[0].transactionHash").value("tx_pending_1"));
    }

    private Block saveBlock(
            Integer height,
            String blockHash,
            String previousHash,
            Block.Status status,
            LocalDateTime timestamp) {
        Block block = new Block();
        block.setBlockHeight(height);
        block.setBlockHash(blockHash);
        block.setPreviousHash(previousHash);
        block.setValidatorAddress("validator_1");
        block.setTimestamp(timestamp);
        block.setTransactionCount(0);
        block.setStatus(status);
        return blockRepository.save(block);
    }

    private Transaction saveTransaction(
            String txHash,
            Integer blockId,
            Transaction.Status status,
            Integer nonce) {
        Transaction tx = new Transaction();
        tx.setSenderAddress("sender_wallet");
        tx.setReceiverAddress("receiver_wallet");
        tx.setAmount(new BigDecimal("1.25000000"));
        tx.setFee(new BigDecimal("0.01000000"));
        tx.setNonce(nonce);
        tx.setTimestamp(LocalDateTime.of(2026, 3, 1, 12, 0));
        tx.setTransactionHash(txHash);
        tx.setStatus(status);
        tx.setBlockId(blockId);
        return transactionRepository.save(tx);
    }

    private void saveBlockTransaction(Integer blockId, Integer transactionId) {
        BlockTransaction join = new BlockTransaction();
        join.setBlockId(blockId);
        join.setTransactionId(transactionId);
        blockTransactionRepository.save(join);
    }
}
