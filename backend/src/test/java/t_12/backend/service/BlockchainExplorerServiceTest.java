package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.api.blockchain.dto.BlockDetailDTO;
import t_12.backend.api.blockchain.dto.ChainStatusDTO;
import t_12.backend.entity.Block;
import t_12.backend.entity.Transaction;
import t_12.backend.repository.BlockRepository;
import t_12.backend.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class BlockchainExplorerServiceTest {

    @Mock
    private BlockService blockService;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BlockchainExplorerService blockchainExplorerService;

    @Test
    void getBlockByHeight_includesLinkedTransactions() {
        Block block = new Block();
        block.setId(2);
        block.setBlockHeight(1);
        block.setPreviousHash("prev_hash");
        block.setBlockHash("block_hash");
        block.setValidatorAddress("validator_1");
        block.setTimestamp(LocalDateTime.of(2026, 3, 22, 10, 0, 0));
        block.setTransactionCount(1);
        block.setStatus(Block.Status.COMMITTED);

        Transaction tx = new Transaction();
        tx.setId(10);
        tx.setTransactionHash("tx_hash_1");
        tx.setSenderAddress("sender_1");
        tx.setReceiverAddress("receiver_1");
        tx.setAmount(new BigDecimal("5.00000000"));
        tx.setFee(new BigDecimal("0.01000000"));
        tx.setNonce(3);
        tx.setTimestamp(LocalDateTime.of(2026, 3, 22, 9, 59, 0));
        tx.setStatus(Transaction.Status.CONFIRMED);

        when(blockService.findByHeight(1)).thenReturn(block);
        when(blockService.getBlockTransactions(2)).thenReturn(List.of(tx));

        BlockDetailDTO result = blockchainExplorerService.getBlockByHeight(1);

        assertEquals(1, result.getBlockHeight());
        assertEquals("block_hash", result.getBlockHash());
        assertEquals(1, result.getTransactions().size());
        assertEquals("tx_hash_1", result.getTransactions().get(0).getTransactionHash());
    }

    @Test
    void getChainStatus_returnsLatestAndCounts() {
        Block latest = new Block();
        latest.setBlockHeight(9);
        latest.setBlockHash("latest_hash");
        latest.setTimestamp(LocalDateTime.of(2026, 3, 22, 12, 0, 0));

        when(blockRepository.findTopByOrderByBlockHeightDesc()).thenReturn(Optional.of(latest));
        when(blockRepository.count()).thenReturn(10L);
        when(blockRepository.countByStatus(Block.Status.COMMITTED)).thenReturn(10L);
        when(transactionRepository.countByStatus(Transaction.Status.PENDING)).thenReturn(4L);

        ChainStatusDTO result = blockchainExplorerService.getChainStatus();

        assertEquals(10L, result.getTotalBlocks());
        assertEquals(10L, result.getCommittedBlocks());
        assertEquals(4L, result.getPendingTransactions());
        assertEquals(9, result.getLatestBlockHeight());
        assertEquals("latest_hash", result.getLatestBlockHash());
    }

    @Test
    void getChainStatus_emptyChain_returnsNullLatestBlockInfo() {
        when(blockRepository.findTopByOrderByBlockHeightDesc()).thenReturn(Optional.empty());
        when(blockRepository.count()).thenReturn(0L);
        when(blockRepository.countByStatus(Block.Status.COMMITTED)).thenReturn(0L);
        when(transactionRepository.countByStatus(Transaction.Status.PENDING)).thenReturn(0L);

        ChainStatusDTO result = blockchainExplorerService.getChainStatus();

        assertEquals(0L, result.getTotalBlocks());
        assertEquals(0L, result.getCommittedBlocks());
        assertEquals(0L, result.getPendingTransactions());
        assertNull(result.getLatestBlockHeight());
        assertNull(result.getLatestBlockHash());
        assertNull(result.getLatestBlockTimestamp());
    }
}
