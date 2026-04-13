package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import t_12.backend.api.blockchain.dto.ChainValidationReportDTO;
import t_12.backend.entity.Block;
import t_12.backend.entity.BlockTransaction;
import t_12.backend.entity.Transaction;
import t_12.backend.repository.BlockRepository;
import t_12.backend.repository.BlockTransactionRepository;
import t_12.backend.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class ChainValidationServiceTest {

    private static final String GENESIS_PREVIOUS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private BlockTransactionRepository blockTransactionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BlockService blockService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private ChainValidationService chainValidationService;

    @Test
    void validateFullChain_validChain_passesCleanly() {
        LocalDateTime genesisTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime blockTime = LocalDateTime.of(2026, 1, 1, 0, 2, 0);

        Block genesis = block(1, 0, GENESIS_PREVIOUS_HASH, "genesis_hash", genesisTime);
        Block block1 = block(2, 1, "genesis_hash", "block_1_hash", blockTime);
        Transaction tx = transaction(
                10,
                "sender_wallet",
                "receiver_wallet",
                new BigDecimal("1.50000000"),
                new BigDecimal("0.01000000"),
                3,
                LocalDateTime.of(2026, 1, 1, 0, 1, 0),
                "tx_hash_10"
        );

        when(blockRepository.findAllByOrderByBlockHeightAsc()).thenReturn(List.of(genesis, block1));
        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(1)).thenReturn(List.of());
        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(2))
                .thenReturn(List.of(blockTransaction(100, 2, 10)));
        when(transactionRepository.findById(10)).thenReturn(Optional.of(tx));
        when(transactionService.generateTransactionHash(
                tx.getSenderAddress(),
                tx.getReceiverAddress(),
                tx.getAmount(),
                tx.getFee(),
                tx.getNonce(),
                tx.getTimestamp()
        )).thenReturn("tx_hash_10");
        when(blockService.generateBlockHash(0, GENESIS_PREVIOUS_HASH, genesisTime, List.of()))
                .thenReturn("genesis_hash");
        when(blockService.generateBlockHash(1, "genesis_hash", blockTime, List.of("tx_hash_10")))
                .thenReturn("block_1_hash");

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertTrue(report.isValid());
        assertEquals(2, report.getCheckedBlocks());
        assertEquals(1, report.getCheckedTransactions());
        assertNull(report.getFailure());
    }

    @Test
    void validateFullChain_tamperedBlockHash_failsWithExactBlock() {
        LocalDateTime genesisTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime blockTime = LocalDateTime.of(2026, 1, 1, 0, 2, 0);

        Block genesis = block(1, 0, GENESIS_PREVIOUS_HASH, "genesis_hash", genesisTime);
        Block tampered = block(2, 1, "genesis_hash", "tampered_hash", blockTime);

        when(blockRepository.findAllByOrderByBlockHeightAsc()).thenReturn(List.of(genesis, tampered));
        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(1)).thenReturn(List.of());
        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(2)).thenReturn(List.of());
        when(blockService.generateBlockHash(0, GENESIS_PREVIOUS_HASH, genesisTime, List.of()))
                .thenReturn("genesis_hash");
        when(blockService.generateBlockHash(1, "genesis_hash", blockTime, List.of()))
                .thenReturn("expected_block_1_hash");

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFalse(report.isValid());
        assertEquals(2, report.getCheckedBlocks());
        assertNotNull(report.getFailure());
        assertEquals(1, report.getFailure().getBlockHeight());
        assertEquals("BLOCK_HASH_MISMATCH", report.getFailure().getCode());
    }

    @Test
    void validateFullChain_brokenPreviousHashLink_failsWithExactBlock() {
        LocalDateTime genesisTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime blockTime = LocalDateTime.of(2026, 1, 1, 0, 2, 0);

        Block genesis = block(1, 0, GENESIS_PREVIOUS_HASH, "genesis_hash", genesisTime);
        Block brokenLinkBlock = block(2, 1, "wrong_previous_hash", "block_1_hash", blockTime);

        when(blockRepository.findAllByOrderByBlockHeightAsc()).thenReturn(List.of(genesis, brokenLinkBlock));
        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(1)).thenReturn(List.of());
        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(2)).thenReturn(List.of());
        when(blockService.generateBlockHash(0, GENESIS_PREVIOUS_HASH, genesisTime, List.of()))
                .thenReturn("genesis_hash");

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFalse(report.isValid());
        assertEquals(2, report.getCheckedBlocks());
        assertNotNull(report.getFailure());
        assertEquals(1, report.getFailure().getBlockHeight());
        assertEquals("BROKEN_PREVIOUS_HASH_LINK", report.getFailure().getCode());
    }

    @Test
    void validateFullChain_invalidTransactionHash_failsWithExactBlock() {
        LocalDateTime genesisTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime blockTime = LocalDateTime.of(2026, 1, 1, 0, 2, 0);

        Block genesis = block(1, 0, GENESIS_PREVIOUS_HASH, "genesis_hash", genesisTime);
        Block block1 = block(2, 1, "genesis_hash", "block_1_hash", blockTime);
        Transaction tamperedTx = transaction(
                20,
                "sender_wallet",
                "receiver_wallet",
                new BigDecimal("3.00000000"),
                new BigDecimal("0.02000000"),
                5,
                LocalDateTime.of(2026, 1, 1, 0, 1, 0),
                "stored_tx_hash"
        );

        when(blockRepository.findAllByOrderByBlockHeightAsc()).thenReturn(List.of(genesis, block1));
        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(1)).thenReturn(List.of());
        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(2))
                .thenReturn(List.of(blockTransaction(101, 2, 20)));
        when(transactionRepository.findById(20)).thenReturn(Optional.of(tamperedTx));
        when(blockService.generateBlockHash(0, GENESIS_PREVIOUS_HASH, genesisTime, List.of()))
                .thenReturn("genesis_hash");
        when(transactionService.generateTransactionHash(
                tamperedTx.getSenderAddress(),
                tamperedTx.getReceiverAddress(),
                tamperedTx.getAmount(),
                tamperedTx.getFee(),
                tamperedTx.getNonce(),
                tamperedTx.getTimestamp()
        )).thenReturn("expected_tx_hash");

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFalse(report.isValid());
        assertEquals(2, report.getCheckedBlocks());
        assertEquals(1, report.getCheckedTransactions());
        assertNotNull(report.getFailure());
        assertEquals(1, report.getFailure().getBlockHeight());
        assertEquals("INVALID_TRANSACTION_HASH", report.getFailure().getCode());
        assertEquals(20, report.getFailure().getTransactionId());
    }

    private Block block(
            int id,
            int height,
            String previousHash,
            String blockHash,
            LocalDateTime timestamp) {
        Block block = new Block();
        block.setId(id);
        block.setBlockHeight(height);
        block.setPreviousHash(previousHash);
        block.setBlockHash(blockHash);
        block.setTimestamp(timestamp);
        block.setTransactionCount(0);
        block.setStatus(Block.Status.COMMITTED);
        return block;
    }

    private BlockTransaction blockTransaction(int id, int blockId, int txId) {
        BlockTransaction blockTransaction = new BlockTransaction();
        blockTransaction.setId(id);
        blockTransaction.setBlockId(blockId);
        blockTransaction.setTransactionId(txId);
        return blockTransaction;
    }

    private Transaction transaction(
            int id,
            String sender,
            String receiver,
            BigDecimal amount,
            BigDecimal fee,
            int nonce,
            LocalDateTime timestamp,
            String hash) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setSenderAddress(sender);
        tx.setReceiverAddress(receiver);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setNonce(nonce);
        tx.setTimestamp(timestamp);
        tx.setTransactionHash(hash);
        tx.setStatus(Transaction.Status.CONFIRMED);
        return tx;
    }
}
