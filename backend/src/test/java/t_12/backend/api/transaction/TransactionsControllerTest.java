package t_12.backend.api.transaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import t_12.backend.entity.Transaction;
import t_12.backend.exception.GlobalExceptionHandler;
import t_12.backend.exception.InvalidNonceException;
import t_12.backend.service.PurchaseService;
import t_12.backend.service.TransactionHistoryService;
import t_12.backend.service.TransactionService;
import t_12.backend.service.TransactionValidationService;

@ExtendWith(MockitoExtension.class)
class TransactionsControllerTest {

    @Mock
    private TransactionHistoryService transactionHistoryService;

    @Mock
    private PurchaseService purchaseService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TransactionValidationService validationService;

    @InjectMocks
    private TransactionsController transactionsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void submitTransfer_validRequest_returnsCreatedTransaction() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setSenderAddress("wlt_sender_address");
        request.setReceiverAddress("wlt_receiver_address");
        request.setAmount(new BigDecimal("5.00000000"));
        request.setFee(new BigDecimal("0.01000000"));
        request.setNonce(1);
        String requestJson = """
                {
                  "senderAddress": "wlt_sender_address",
                  "receiverAddress": "wlt_receiver_address",
                  "amount": 5.00000000,
                  "fee": 0.01000000,
                  "nonce": 1
                }
                """;

        Transaction tx = new Transaction();
        tx.setSenderAddress(request.getSenderAddress());
        tx.setReceiverAddress(request.getReceiverAddress());
        tx.setAmount(request.getAmount());
        tx.setFee(request.getFee());
        tx.setNonce(request.getNonce());
        tx.setStatus(Transaction.Status.PENDING);
        tx.setTransactionHash("tx_hash_1");
        tx.setBlockId(null);

        when(transactionService.createTransaction(
                request.getSenderAddress(),
                request.getReceiverAddress(),
                request.getAmount(),
                request.getFee(),
                request.getNonce()
        )).thenReturn(tx);

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderAddress").value(request.getSenderAddress()))
                .andExpect(jsonPath("$.receiverAddress").value(request.getReceiverAddress()))
                .andExpect(jsonPath("$.nonce").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(validationService).validateBalance(
                request.getSenderAddress(),
                request.getAmount(),
                request.getFee()
        );
        verify(transactionService).createTransaction(
                request.getSenderAddress(),
                request.getReceiverAddress(),
                request.getAmount(),
                request.getFee(),
                request.getNonce()
        );
    }

    @Test
    void submitTransfer_invalidNonce_returnsStructuredBadRequest() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setSenderAddress("wlt_sender_address");
        request.setReceiverAddress("wlt_receiver_address");
        request.setAmount(new BigDecimal("5.00000000"));
        request.setFee(new BigDecimal("0.01000000"));
        request.setNonce(3);
        String requestJson = """
                {
                  "senderAddress": "wlt_sender_address",
                  "receiverAddress": "wlt_receiver_address",
                  "amount": 5.00000000,
                  "fee": 0.01000000,
                  "nonce": 3
                }
                """;

        when(transactionService.createTransaction(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidNonceException(4L, 3));

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid nonce: expected 4 but received 3."))
                .andExpect(jsonPath("$.expectedNonce").value(4))
                .andExpect(jsonPath("$.providedNonce").value(3));
    }
}
