package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import t_12.backend.api.blockchain.dto.BlockDetailDTO;
import t_12.backend.api.blockchain.dto.BlockListResponseDTO;
import t_12.backend.api.blockchain.dto.ChainStatusDTO;
import t_12.backend.entity.Block;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.ApiException;
import t_12.backend.repository.BlockRepository;
import t_12.backend.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class BlockchainExplorerServiceTest {

    @Mock
    private BlockService blockService;

    @Mock
    private MempoolService mempoolService;

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

    @Test
    void getRecentBlocks_returnsNewestFirstWithPagination() {
        Block block9 = new Block();
        block9.setBlockHeight(9);
        block9.setBlockHash("hash9");
        block9.setPreviousHash("hash8");
        block9.setTransactionCount(2);
        block9.setStatus(Block.Status.COMMITTED);
        block9.setTimestamp(LocalDateTime.of(2026, 3, 22, 12, 0, 0));

        Block block8 = new Block();
        block8.setBlockHeight(8);
        block8.setBlockHash("hash8");
        block8.setPreviousHash("hash7");
        block8.setTransactionCount(1);
        block8.setStatus(Block.Status.COMMITTED);
        block8.setTimestamp(LocalDateTime.of(2026, 3, 22, 11, 0, 0));

        when(blockRepository.findAllByOrderByBlockHeightDesc(PageRequest.of(0, 2)))
                .thenReturn(new PageImpl<>(List.of(block9, block8), PageRequest.of(0, 2), 9));

        BlockListResponseDTO result = blockchainExplorerService.getRecentBlocks(1, 2);

        assertEquals(2, result.getData().size());
        assertEquals(9, result.getData().get(0).getBlockHeight());
        assertEquals(8, result.getData().get(1).getBlockHeight());
        assertEquals(1, result.getPagination().getPage());
        assertEquals(2, result.getPagination().getLimit());
        assertEquals(9, result.getPagination().getTotal());
        assertEquals(5, result.getPagination().getTotalPages());
    }

    @Test
    void getRecentBlocks_invalidPage_throwsBadRequest() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> blockchainExplorerService.getRecentBlocks(0, 20)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("page must be a positive integer", exception.getMessage());
    }

    @Test
    void getRecentBlocks_invalidLimit_throwsBadRequest() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> blockchainExplorerService.getRecentBlocks(1, 0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("limit must be an integer between 1 and 100", exception.getMessage());
    }

    @Test
    void minePendingTransactions_minesNewBlockFromPendingPool() {
        Transaction pending1 = new Transaction();
        pending1.setId(10);
        pending1.setTransactionHash("tx_hash_1");
        pending1.setTimestamp(LocalDateTime.of(2026, 3, 22, 10, 0, 0));

        Transaction pending2 = new Transaction();
        pending2.setId(11);
        pending2.setTransactionHash("tx_hash_2");
        pending2.setTimestamp(LocalDateTime.of(2026, 3, 22, 10, 1, 0));

        Block mined = new Block();
        mined.setId(3);
        mined.setBlockHeight(2);
        mined.setBlockHash("mined_hash");
        mined.setPreviousHash("prev_hash");
        mined.setTimestamp(LocalDateTime.of(2026, 3, 22, 10, 2, 0));
        mined.setStatus(Block.Status.COMMITTED);
        mined.setTransactionCount(2);

        when(mempoolService.getPendingTransactions()).thenReturn(List.of(pending2, pending1));
        when(blockRepository.count()).thenReturn(1L);
        when(blockService.createBlock(anyList(), eq("validator_addr"))).thenReturn(mined);
        when(blockService.getBlockTransactions(3)).thenReturn(List.of(pending1, pending2));

        BlockDetailDTO result = blockchainExplorerService.minePendingTransactions(10, "validator_addr");

        assertEquals(2, result.getBlockHeight());
        assertEquals("mined_hash", result.getBlockHash());
        assertEquals(2, result.getTransactions().size());
        verify(blockService, never()).createGenesisBlock();
    }

    @Test
    void minePendingTransactions_emptyPool_throwsConflict() {
        when(mempoolService.getPendingTransactions()).thenReturn(List.of());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> blockchainExplorerService.minePendingTransactions(10, null)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("No pending transactions to mine", exception.getMessage());
    }

    @Test
    void minePendingTransactions_invalidLimit_throwsBadRequest() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> blockchainExplorerService.minePendingTransactions(0, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("limit must be an integer between 1 and 1000", exception.getMessage());
    }
}
