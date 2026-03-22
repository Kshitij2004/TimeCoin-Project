package t_12.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import t_12.backend.entity.Block;
import t_12.backend.entity.BlockTransaction;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.BlockRepository;
import t_12.backend.repository.BlockTransactionRepository;
import t_12.backend.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private BlockTransactionRepository blockTransactionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BlockService blockService;

    private static final String GENESIS_PREV_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";
    private static final LocalDateTime GENESIS_TIMESTAMP =
            LocalDateTime.of(2026, 1, 1, 0, 0, 0);

    private Block genesisBlock;

    @BeforeEach
    void setUp() {
        genesisBlock = new Block();
        genesisBlock.setId(1);
        genesisBlock.setBlockHeight(0);
        genesisBlock.setPreviousHash(GENESIS_PREV_HASH);
        genesisBlock.setBlockHash("abc123genesishash");
        genesisBlock.setTimestamp(LocalDateTime.of(2026, 3, 15, 12, 0, 0));
        genesisBlock.setTransactionCount(0);
        genesisBlock.setStatus(Block.Status.COMMITTED);
    }

    // Genesis Block

    @Test
    void createGenesisBlock_emptyChain_createsBlockAtHeight0() {
        when(blockRepository.existsByBlockHeight(0)).thenReturn(false);
        when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
            Block b = inv.getArgument(0);
            b.setId(1);
            return b;
        });

        Block result = blockService.createGenesisBlock();

        assertEquals(0, result.getBlockHeight());
        assertEquals(GENESIS_PREV_HASH, result.getPreviousHash());
        assertEquals(0, result.getTransactionCount());
        assertEquals(Block.Status.COMMITTED, result.getStatus());
        assertNotNull(result.getBlockHash());
        assertEquals(GENESIS_TIMESTAMP, result.getTimestamp());
        assertEquals(
                blockService.generateBlockHash(0, GENESIS_PREV_HASH, GENESIS_TIMESTAMP, List.of()),
                result.getBlockHash()
        );
        verify(blockRepository).save(any(Block.class));
    }

    @Test
    void createGenesisBlock_alreadyExists_returnsExisting() {
        when(blockRepository.existsByBlockHeight(0)).thenReturn(true);
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(genesisBlock));

        Block result = blockService.createGenesisBlock();

        assertEquals(genesisBlock.getId(), result.getId());
        verify(blockRepository, never()).save(any(Block.class));
    }

    @Test
    void createGenesisBlock_hashIsValidSHA256() {
        when(blockRepository.existsByBlockHeight(0)).thenReturn(false);
        when(blockRepository.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

        Block result = blockService.createGenesisBlock();

        assertEquals(64, result.getBlockHash().length());
        assertTrue(result.getBlockHash().matches("[0-9a-f]+"));
    }

    @Test
    void createGenesisBlock_duplicateInsertRace_returnsExistingBlock() {
        when(blockRepository.existsByBlockHeight(0)).thenReturn(false);
        when(blockRepository.save(any(Block.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate genesis"));
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(genesisBlock));

        Block result = blockService.createGenesisBlock();

        assertEquals(genesisBlock.getId(), result.getId());
        verify(blockRepository).save(any(Block.class));
    }

    // Block Hash Generation

    @Test
    void generateBlockHash_sameInputs_producesSameHash() {
        LocalDateTime ts = LocalDateTime.of(2026, 3, 15, 12, 0, 0);
        List<String> txHashes = List.of("txhash1", "txhash2");

        String hash1 = blockService.generateBlockHash(1, "prevhash", ts, txHashes);
        String hash2 = blockService.generateBlockHash(1, "prevhash", ts, txHashes);

        assertEquals(hash1, hash2);
    }

    @Test
    void generateBlockHash_differentHeight_producesDifferentHash() {
        LocalDateTime ts = LocalDateTime.of(2026, 3, 15, 12, 0, 0);
        List<String> txHashes = List.of("txhash1");

        String hash1 = blockService.generateBlockHash(1, "prevhash", ts, txHashes);
        String hash2 = blockService.generateBlockHash(2, "prevhash", ts, txHashes);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void generateBlockHash_differentPreviousHash_producesDifferentHash() {
        LocalDateTime ts = LocalDateTime.of(2026, 3, 15, 12, 0, 0);
        List<String> txHashes = List.of("txhash1");

        String hash1 = blockService.generateBlockHash(1, "prevhashA", ts, txHashes);
        String hash2 = blockService.generateBlockHash(1, "prevhashB", ts, txHashes);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void generateBlockHash_differentTransactions_producesDifferentHash() {
        LocalDateTime ts = LocalDateTime.of(2026, 3, 15, 12, 0, 0);

        String hash1 = blockService.generateBlockHash(1, "prev", ts, List.of("tx1"));
        String hash2 = blockService.generateBlockHash(1, "prev", ts, List.of("tx2"));

        assertNotEquals(hash1, hash2);
    }

    @Test
    void generateBlockHash_differentTimestamp_producesDifferentHash() {
        LocalDateTime ts1 = LocalDateTime.of(2026, 3, 15, 12, 0, 0);
        LocalDateTime ts2 = LocalDateTime.of(2026, 3, 15, 12, 0, 1);

        String hash1 = blockService.generateBlockHash(1, "prev", ts1, List.of("tx1"));
        String hash2 = blockService.generateBlockHash(1, "prev", ts2, List.of("tx1"));

        assertNotEquals(hash1, hash2);
    }

    @Test
    void generateBlockHash_emptyTransactions_stillProducesValidHash() {
        LocalDateTime ts = LocalDateTime.of(2026, 3, 15, 12, 0, 0);

        String hash = blockService.generateBlockHash(0, GENESIS_PREV_HASH, ts, List.of());

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    void generateBlockHash_transactionOrderMatters() {
        LocalDateTime ts = LocalDateTime.of(2026, 3, 15, 12, 0, 0);

        String hash1 = blockService.generateBlockHash(1, "prev", ts, List.of("txA", "txB"));
        String hash2 = blockService.generateBlockHash(1, "prev", ts, List.of("txB", "txA"));

        assertNotEquals(hash1, hash2);
    }

    // Create Block

    @Test
    void createBlock_linksToLatestBlock() {
        when(blockRepository.findTopByOrderByBlockHeightDesc())
                .thenReturn(Optional.of(genesisBlock));
        when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
            Block b = inv.getArgument(0);
            b.setId(2);
            return b;
        });
        when(blockTransactionRepository.save(any(BlockTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = mockTransaction(10, "txhash_aaa");

        Block result = blockService.createBlock(List.of(tx), null);

        assertEquals(1, result.getBlockHeight());
        assertEquals(genesisBlock.getBlockHash(), result.getPreviousHash());
    }

    @Test
    void createBlock_setsCorrectTransactionCount() {
        when(blockRepository.findTopByOrderByBlockHeightDesc())
                .thenReturn(Optional.of(genesisBlock));
        when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
            Block b = inv.getArgument(0);
            b.setId(2);
            return b;
        });
        when(blockTransactionRepository.save(any(BlockTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<Transaction> txs = List.of(
                mockTransaction(10, "hash1"),
                mockTransaction(11, "hash2"),
                mockTransaction(12, "hash3")
        );

        Block result = blockService.createBlock(txs, null);

        assertEquals(3, result.getTransactionCount());
    }

    @Test
    void createBlock_statusIsConfirmed() {
        when(blockRepository.findTopByOrderByBlockHeightDesc())
                .thenReturn(Optional.of(genesisBlock));
        when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
            Block b = inv.getArgument(0);
            b.setId(2);
            return b;
        });
        when(blockTransactionRepository.save(any(BlockTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Block result = blockService.createBlock(List.of(mockTransaction(10, "hash1")), null);

        assertEquals(Block.Status.COMMITTED, result.getStatus());
    }

    @Test
    void createBlock_createsJoinTableEntries() {
        when(blockRepository.findTopByOrderByBlockHeightDesc())
                .thenReturn(Optional.of(genesisBlock));
        when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
            Block b = inv.getArgument(0);
            b.setId(2);
            return b;
        });
        when(blockTransactionRepository.save(any(BlockTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<Transaction> txs = List.of(
                mockTransaction(10, "hash1"),
                mockTransaction(11, "hash2")
        );

        blockService.createBlock(txs, null);

        ArgumentCaptor<BlockTransaction> captor = ArgumentCaptor.forClass(BlockTransaction.class);
        verify(blockTransactionRepository, times(2)).save(captor.capture());

        List<BlockTransaction> saved = captor.getAllValues();
        assertEquals(2, saved.get(0).getBlockId());
        assertEquals(10, saved.get(0).getTransactionId());
        assertEquals(2, saved.get(1).getBlockId());
        assertEquals(11, saved.get(1).getTransactionId());
    }

    @Test
    void createBlock_confirmsAllTransactions() {
        when(blockRepository.findTopByOrderByBlockHeightDesc())
                .thenReturn(Optional.of(genesisBlock));
        when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
            Block b = inv.getArgument(0);
            b.setId(2);
            return b;
        });
        when(blockTransactionRepository.save(any(BlockTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Transaction tx1 = mockTransaction(10, "hash1");
        Transaction tx2 = mockTransaction(11, "hash2");

        blockService.createBlock(List.of(tx1, tx2), null);

        assertEquals(Transaction.Status.CONFIRMED, tx1.getStatus());
        assertEquals(Transaction.Status.CONFIRMED, tx2.getStatus());
        assertEquals(2, tx1.getBlockId());
        assertEquals(2, tx2.getBlockId());
    }

    @Test
    void createBlock_setsValidatorAddress() {
        when(blockRepository.findTopByOrderByBlockHeightDesc())
                .thenReturn(Optional.of(genesisBlock));
        when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
            Block b = inv.getArgument(0);
            b.setId(2);
            return b;
        });
        when(blockTransactionRepository.save(any(BlockTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Block result = blockService.createBlock(
                List.of(mockTransaction(10, "hash1")), "validator_addr_123");

        assertEquals("validator_addr_123", result.getValidatorAddress());
    }

    @Test
    void createBlock_emptyChain_throwsIllegalState() {
        when(blockRepository.findTopByOrderByBlockHeightDesc())
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                blockService.createBlock(List.of(mockTransaction(10, "hash1")), null));
    }

    @Test
    void createBlock_hashIsValidSHA256() {
        when(blockRepository.findTopByOrderByBlockHeightDesc())
                .thenReturn(Optional.of(genesisBlock));
        when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
            Block b = inv.getArgument(0);
            b.setId(2);
            return b;
        });
        when(blockTransactionRepository.save(any(BlockTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Block result = blockService.createBlock(
                List.of(mockTransaction(10, "hash1")), null);

        assertEquals(64, result.getBlockHash().length());
        assertTrue(result.getBlockHash().matches("[0-9a-f]+"));
    }

    // Chain Linking

    @Test
    void createBlock_secondBlock_linksToFirst() {
        // simulate block at height 1 already exists
        Block block1 = new Block();
        block1.setId(2);
        block1.setBlockHeight(1);
        block1.setBlockHash("block1hash_abc");
        block1.setTimestamp(LocalDateTime.of(2026, 3, 15, 13, 0, 0));

        when(blockRepository.findTopByOrderByBlockHeightDesc())
                .thenReturn(Optional.of(block1));
        when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
            Block b = inv.getArgument(0);
            b.setId(3);
            return b;
        });
        when(blockTransactionRepository.save(any(BlockTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Block result = blockService.createBlock(
                List.of(mockTransaction(20, "txhash_new")), null);

        assertEquals(2, result.getBlockHeight());
        assertEquals("block1hash_abc", result.getPreviousHash());
    }

    // Lookup Methods 

    @Test
    void findByHeight_existingHeight_returnsBlock() {
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(genesisBlock));

        Block result = blockService.findByHeight(0);

        assertEquals(0, result.getBlockHeight());
    }

    @Test
    void findByHeight_nonExistent_throwsException() {
        when(blockRepository.findByBlockHeight(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> blockService.findByHeight(999));
    }

    @Test
    void findByHash_existingHash_returnsBlock() {
        when(blockRepository.findByBlockHash("abc123genesishash"))
                .thenReturn(Optional.of(genesisBlock));

        Block result = blockService.findByHash("abc123genesishash");

        assertNotNull(result);
    }

    @Test
    void findByHash_nonExistent_throwsException() {
        when(blockRepository.findByBlockHash("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> blockService.findByHash("nonexistent"));
    }

    @Test
    void findById_existingId_returnsBlock() {
        when(blockRepository.findById(1)).thenReturn(Optional.of(genesisBlock));

        Block result = blockService.findById(1);

        assertEquals(1, result.getId());
    }

    @Test
    void findById_nonExistent_throwsException() {
        when(blockRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> blockService.findById(999));
    }

    @Test
    void getLatestBlock_returnsHighestBlock() {
        when(blockRepository.findTopByOrderByBlockHeightDesc())
                .thenReturn(Optional.of(genesisBlock));

        Block result = blockService.getLatestBlock();

        assertEquals(genesisBlock.getBlockHeight(), result.getBlockHeight());
    }

    @Test
    void getLatestBlock_emptyChain_throwsException() {
        when(blockRepository.findTopByOrderByBlockHeightDesc())
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> blockService.getLatestBlock());
    }

    // Get Block Transactions

    @Test
    void getBlockTransactions_returnsAssociatedTransactions() {
        BlockTransaction bt1 = new BlockTransaction();
        bt1.setBlockId(1);
        bt1.setTransactionId(10);

        BlockTransaction bt2 = new BlockTransaction();
        bt2.setBlockId(1);
        bt2.setTransactionId(11);

        Transaction tx1 = mockTransaction(10, "hash1");
        Transaction tx2 = mockTransaction(11, "hash2");

        when(blockTransactionRepository.findByBlockId(1)).thenReturn(List.of(bt1, bt2));
        when(transactionRepository.findById(10)).thenReturn(Optional.of(tx1));
        when(transactionRepository.findById(11)).thenReturn(Optional.of(tx2));

        List<Transaction> result = blockService.getBlockTransactions(1);

        assertEquals(2, result.size());
    }

    @Test
    void getBlockTransactions_noTransactions_returnsEmptyList() {
        when(blockTransactionRepository.findByBlockId(1)).thenReturn(List.of());

        List<Transaction> result = blockService.getBlockTransactions(1);

        assertTrue(result.isEmpty());
    }

    // Helper

    private Transaction mockTransaction(Integer id, String hash) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setTransactionHash(hash);
        tx.setSenderAddress("sender_addr");
        tx.setReceiverAddress("receiver_addr");
        tx.setAmount(new BigDecimal("10.00"));
        tx.setFee(new BigDecimal("0.01"));
        tx.setNonce(1);
        tx.setTimestamp(LocalDateTime.of(2026, 3, 15, 12, 0, 0));
        tx.setStatus(Transaction.Status.PENDING);
        return tx;
    }
}
