package t_12.backend.integration;

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
import t_12.backend.entity.Transaction;
import t_12.backend.entity.User;
import t_12.backend.entity.Wallet;
import t_12.backend.repository.TransactionRepository;
import t_12.backend.repository.UserRepository;
import t_12.backend.repository.WalletRepository;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:transaction_api_test_db;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class TransactionApiIntegrationTest {

    private static final String JWT_SECRET = "your-super-secret-key-change-this-in-production";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void transfer_withValidNextNonce_isAccepted() throws Exception {
        Wallet senderWallet = createUserAndWallet("sender1", "sender1@example.com", "wlt_sender_1");
        seedConfirmedCredit(senderWallet.getWalletAddress(), "seed_hash_1");
        String token = tokenForUser(senderWallet.getUserId());

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(transferPayload(
                                senderWallet.getWalletAddress(),
                                "wlt_receiver_1",
                                "5.00000000",
                                "0.01000000",
                                1
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderAddress").value(senderWallet.getWalletAddress()))
                .andExpect(jsonPath("$.receiverAddress").value("wlt_receiver_1"))
                .andExpect(jsonPath("$.nonce").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void transfer_withDuplicateNonce_isRejectedWithExpectedAndProvided() throws Exception {
        Wallet senderWallet = createUserAndWallet("sender2", "sender2@example.com", "wlt_sender_2");
        seedConfirmedCredit(senderWallet.getWalletAddress(), "seed_hash_2");
        String token = tokenForUser(senderWallet.getUserId());

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(transferPayload(
                                senderWallet.getWalletAddress(),
                                "wlt_receiver_2",
                                "3.00000000",
                                "0.01000000",
                                1
                        )))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(transferPayload(
                                senderWallet.getWalletAddress(),
                                "wlt_receiver_2b",
                                "1.00000000",
                                "0.01000000",
                                1
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid nonce: expected 2 but received 1."))
                .andExpect(jsonPath("$.expectedNonce").value(2))
                .andExpect(jsonPath("$.providedNonce").value(1));
    }

    @Test
    void transfer_withSkippedNonce_isRejectedWithExpectedAndProvided() throws Exception {
        Wallet senderWallet = createUserAndWallet("sender3", "sender3@example.com", "wlt_sender_3");
        seedConfirmedCredit(senderWallet.getWalletAddress(), "seed_hash_3");
        String token = tokenForUser(senderWallet.getUserId());

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(transferPayload(
                                senderWallet.getWalletAddress(),
                                "wlt_receiver_3",
                                "2.00000000",
                                "0.01000000",
                                1
                        )))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(transferPayload(
                                senderWallet.getWalletAddress(),
                                "wlt_receiver_3b",
                                "1.00000000",
                                "0.01000000",
                                3
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid nonce: expected 2 but received 3."))
                .andExpect(jsonPath("$.expectedNonce").value(2))
                .andExpect(jsonPath("$.providedNonce").value(3));
    }

    private Wallet createUserAndWallet(String username, String email, String walletAddress) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$testtesttesttesttestteuG31xY4nVjc8NQxvH8Yh3YdH5wQhQ6");
        user.setCreatedAt(LocalDateTime.of(2026, 4, 1, 9, 0));
        User savedUser = userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUserId(savedUser.getId());
        wallet.setWalletAddress(walletAddress);
        wallet.setPublicKey("pub_" + walletAddress);
        wallet.setCoinBalance(BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.of(2026, 4, 1, 9, 1));
        return walletRepository.save(wallet);
    }

    private void seedConfirmedCredit(String receiverAddress, String txHash) {
        Transaction tx = new Transaction();
        tx.setSenderAddress(null);
        tx.setReceiverAddress(receiverAddress);
        tx.setAmount(new BigDecimal("100.00000000"));
        tx.setFee(BigDecimal.ZERO.setScale(8));
        tx.setNonce(0);
        tx.setTimestamp(LocalDateTime.of(2026, 4, 1, 9, 2));
        tx.setTransactionHash(txHash);
        tx.setStatus(Transaction.Status.CONFIRMED);
        tx.setBlockId(null);
        transactionRepository.save(tx);
    }

    private String tokenForUser(Integer userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()))
                .compact();
    }

    private String transferPayload(
            String senderAddress,
            String receiverAddress,
            String amount,
            String fee,
            int nonce) {
        return """
                {
                  "senderAddress": "%s",
                  "receiverAddress": "%s",
                  "amount": %s,
                  "fee": %s,
                  "nonce": %d
                }
                """.formatted(senderAddress, receiverAddress, amount, fee, nonce);
    }
}
