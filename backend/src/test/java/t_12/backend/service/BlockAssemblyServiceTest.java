package t_12.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.entity.Block;
import t_12.backend.entity.Transaction;

/**
 * Unit tests for BlockAssemblerService.assembleAndCommit().
 *
 * Uses Mockito to mock MempoolService and BlockService so no database
 * connection is required.
 */
@ExtendWith(MockitoExtension.class)
class BlockAssemblyServiceTest {

    @Mock
    private MempoolService mempoolService;

    @Mock
    private BlockService blockService;

    @InjectMocks
    private BlockAssemblerService blockAssemblerService;

    private Transaction pendingTx;
    private Block committedBlock;

    @BeforeEach
    void setUp() {
        // A sample PENDING transaction to represent mempool contents
        pendingTx = new Transaction();
        pendingTx.setId(1);
        pendingTx.setSenderAddress("sender-address");
        pendingTx.setReceiverAddress("receiver-address");
        pendingTx.setAmount(new BigDecimal("10.00000000"));
        pendingTx.setFee(BigDecimal.ZERO);
        pendingTx.setNonce(0);
        pendingTx.setTimestamp(LocalDateTime.now());
        pendingTx.setTransactionHash("mock-tx-hash");
        pendingTx.setStatus(Transaction.Status.PENDING);

        // A sample committed block returned by BlockService
        committedBlock = new Block();
        committedBlock.setId(1);
        committedBlock.setBlockHeight(1);
        committedBlock.setPreviousHash("genesis-hash");
        committedBlock.setBlockHash("new-block-hash");
        committedBlock.setTransactionCount(1);
        committedBlock.setStatus(Block.Status.COMMITTED);
        committedBlock.setTimestamp(LocalDateTime.now());
    }

    /**
     * When the mempool has pending transactions, assembleAndCommit should
     * delegate to BlockService and return the committed block.
     */
    @Test
    void assembleAndCommit_success_withPendingTransactions() {
        when(mempoolService.getPendingTransactions()).thenReturn(List.of(pendingTx));
        when(blockService.createBlock(List.of(pendingTx), "validator-address"))
                .thenReturn(committedBlock);

        Block result = blockAssemblerService.assembleAndCommit("validator-address");

        // Block should be returned with correct details
        assertNotNull(result);
        assertEquals(1, result.getBlockHeight());
        assertEquals("new-block-hash", result.getBlockHash());
        assertEquals(Block.Status.COMMITTED, result.getStatus());
        assertEquals(1, result.getTransactionCount());

        // BlockService should have been called with the pending transactions
        verify(blockService).createBlock(List.of(pendingTx), "validator-address");
    }

    /**
     * Should work with a null validatorAddress — system-triggered assembly.
     */
    @Test
    void assembleAndCommit_success_withNullValidatorAddress() {
        when(mempoolService.getPendingTransactions()).thenReturn(List.of(pendingTx));
        when(blockService.createBlock(List.of(pendingTx), null))
                .thenReturn(committedBlock);

        Block result = blockAssemblerService.assembleAndCommit(null);

        assertNotNull(result);
        verify(blockService).createBlock(List.of(pendingTx), null);
    }

    /**
     * Should package multiple pending transactions into a single block.
     */
    @Test
    void assembleAndCommit_success_withMultiplePendingTransactions() {
        // Create a second pending transaction
        Transaction pendingTx2 = new Transaction();
        pendingTx2.setId(2);
        pendingTx2.setTransactionHash("mock-tx-hash-2");
        pendingTx2.setStatus(Transaction.Status.PENDING);

        Block multiTxBlock = new Block();
        multiTxBlock.setBlockHeight(1);
        multiTxBlock.setTransactionCount(2);
        multiTxBlock.setStatus(Block.Status.COMMITTED);

        List<Transaction> twoTxs = List.of(pendingTx, pendingTx2);
        when(mempoolService.getPendingTransactions()).thenReturn(twoTxs);
        when(blockService.createBlock(twoTxs, "validator-address")).thenReturn(multiTxBlock);

        Block result = blockAssemblerService.assembleAndCommit("validator-address");

        assertNotNull(result);
        assertEquals(2, result.getTransactionCount());
        verify(blockService).createBlock(twoTxs, "validator-address");
    }

    /**
     * Should throw IllegalStateException when the mempool is empty.
     * BlockService should never be called in this case.
     */
    @Test
    void assembleAndCommit_emptyMempool_throwsIllegalStateException() {
        when(mempoolService.getPendingTransactions()).thenReturn(Collections.emptyList());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> blockAssemblerService.assembleAndCommit("validator-address"));

        assertEquals("No pending transactions in mempool. Block assembly aborted.", ex.getMessage());

        // BlockService should never be called if mempool is empty
        verify(blockService, never()).createBlock(any(), anyString());
    }

    /**
     * If BlockService throws exception, the exception
     * should propagate up and trigger a full rollback via @Transactional.
     */
    @Test
    void assembleAndCommit_blockServiceThrows_exceptionPropagates() {
        when(mempoolService.getPendingTransactions()).thenReturn(List.of(pendingTx));
        when(blockService.createBlock(any(), any()))
                .thenThrow(new IllegalStateException(
                        "No blocks in chain. Create genesis block first."));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> blockAssemblerService.assembleAndCommit("validator-address"));

        assertEquals("No blocks in chain. Create genesis block first.", ex.getMessage());
    }
}