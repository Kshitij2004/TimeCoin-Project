package t_12.backend.api.blockchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import t_12.backend.api.blockchain.dto.BlockDetailDTO;
import t_12.backend.api.blockchain.dto.BlockListItemDTO;
import t_12.backend.api.blockchain.dto.BlockListPaginationDTO;
import t_12.backend.api.blockchain.dto.BlockListResponseDTO;
import t_12.backend.api.blockchain.dto.ChainStatusDTO;
import t_12.backend.api.blockchain.dto.ExplorerTransactionDTO;
import t_12.backend.entity.Block;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.ApiException;
import t_12.backend.service.BlockchainExplorerService;

@ExtendWith(MockitoExtension.class)
class BlockchainExplorerControllerTest {

    @Mock
    private BlockchainExplorerService blockchainExplorerService;

    @InjectMocks
    private BlockchainExplorerController blockchainExplorerController;

    @Test
    void getStatus_returnsChainSummary() {
        when(blockchainExplorerService.getChainStatus())
                .thenReturn(new ChainStatusDTO(
                        5,
                        5,
                        3,
                        4,
                        "latest_hash",
                        LocalDateTime.of(2026, 3, 22, 18, 0, 0)
                ));

        ResponseEntity<ChainStatusDTO> response = blockchainExplorerController.getStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().getTotalBlocks());
        assertEquals("latest_hash", response.getBody().getLatestBlockHash());
        verify(blockchainExplorerService).getChainStatus();
    }

    @Test
    void getBlocks_returnsPaginatedRows() {
        Block block = new Block();
        block.setBlockHeight(4);
        block.setBlockHash("hash4");
        block.setPreviousHash("hash3");
        block.setTimestamp(LocalDateTime.of(2026, 3, 22, 17, 30, 0));
        block.setTransactionCount(2);
        block.setStatus(Block.Status.COMMITTED);
        block.setValidatorAddress("validator_1");

        when(blockchainExplorerService.getRecentBlocks(1, 20))
                .thenReturn(new BlockListResponseDTO(
                        List.of(new BlockListItemDTO(block)),
                        new BlockListPaginationDTO(1, 20, 5, 1)
                ));

        ResponseEntity<BlockListResponseDTO> response = blockchainExplorerController.getBlocks(1, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().size());
        assertEquals(4, response.getBody().getData().get(0).getBlockHeight());
        assertEquals(20, response.getBody().getPagination().getLimit());
        verify(blockchainExplorerService).getRecentBlocks(1, 20);
    }

    @Test
    void getBlockByHeight_returnsTransactions() {
        Block block = new Block();
        block.setId(2);
        block.setBlockHeight(1);
        block.setPreviousHash("hash0");
        block.setBlockHash("hash1");
        block.setValidatorAddress("validator_1");
        block.setTimestamp(LocalDateTime.of(2026, 3, 22, 12, 0, 0));
        block.setTransactionCount(1);
        block.setStatus(Block.Status.COMMITTED);

        Transaction tx = new Transaction();
        tx.setId(10);
        tx.setTransactionHash("tx_hash_1");
        tx.setSenderAddress("sender_1");
        tx.setReceiverAddress("receiver_1");
        tx.setNonce(1);
        tx.setStatus(Transaction.Status.CONFIRMED);

        when(blockchainExplorerService.getBlockByHeight(1))
                .thenReturn(new BlockDetailDTO(block, List.of(new ExplorerTransactionDTO(tx))));

        ResponseEntity<BlockDetailDTO> response = blockchainExplorerController.getBlockByHeight(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getBlockHeight());
        assertEquals("hash1", response.getBody().getBlockHash());
        assertEquals("tx_hash_1", response.getBody().getTransactions().get(0).getTransactionHash());
        verify(blockchainExplorerService).getBlockByHeight(1);
    }

    @Test
    void getBlockByHash_resolvesCorrectBlock() {
        Block block = new Block();
        block.setId(3);
        block.setBlockHeight(2);
        block.setPreviousHash("hash1");
        block.setBlockHash("hash2");
        block.setTimestamp(LocalDateTime.of(2026, 3, 22, 13, 0, 0));
        block.setTransactionCount(0);
        block.setStatus(Block.Status.COMMITTED);

        when(blockchainExplorerService.getBlockByHash("hash2"))
                .thenReturn(new BlockDetailDTO(block, List.of()));

        ResponseEntity<BlockDetailDTO> response = blockchainExplorerController.getBlockByHash("hash2");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getBlockHeight());
        assertEquals("hash2", response.getBody().getBlockHash());
        verify(blockchainExplorerService).getBlockByHash("hash2");
    }

    @Test
    void getBlocks_bubblesServiceValidationError() {
        when(blockchainExplorerService.getRecentBlocks(anyInt(), anyInt()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "page must be a positive integer"));

        ApiException ex = assertThrows(ApiException.class, () -> blockchainExplorerController.getBlocks(0, 20));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("page must be a positive integer", ex.getMessage());

        verify(blockchainExplorerService).getRecentBlocks(0, 20);
    }

    @Test
    void minePending_returnsCreatedBlockDetail() {
        Block mined = new Block();
        mined.setId(2);
        mined.setBlockHeight(1);
        mined.setPreviousHash("genesis_hash");
        mined.setBlockHash("mined_hash");
        mined.setTimestamp(LocalDateTime.of(2026, 3, 22, 20, 0, 0));
        mined.setTransactionCount(1);
        mined.setStatus(Block.Status.COMMITTED);

        Transaction tx = new Transaction();
        tx.setId(100);
        tx.setTransactionHash("tx_hash_mined");
        tx.setSenderAddress("sender_addr");
        tx.setReceiverAddress("receiver_addr");
        tx.setNonce(1);
        tx.setStatus(Transaction.Status.CONFIRMED);

        when(blockchainExplorerService.minePendingTransactions(10, "validator_addr"))
                .thenReturn(new BlockDetailDTO(mined, List.of(new ExplorerTransactionDTO(tx))));

        ResponseEntity<BlockDetailDTO> response = blockchainExplorerController.minePending(10, "validator_addr");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getBlockHeight());
        assertEquals("mined_hash", response.getBody().getBlockHash());
        assertEquals(1, response.getBody().getTransactions().size());
        verify(blockchainExplorerService).minePendingTransactions(10, "validator_addr");
    }
}
