package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
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

    private static final String ZERO_HASH =
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
    void validateFullChain_validChain_passes() {
        ChainFixture fixture = arrangeValidFullChain();

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertTrue(report.isValid());
        assertEquals(3, report.getCheckedBlocks());
        assertEquals(3, report.getCheckedTransactions());
        assertNull(report.getFailure());
        assertEquals(0, fixture.genesis.getBlockHeight());
    }

    @Test
    void validateFullChain_rejectsNonCommittedBlockStatus() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.block1.setStatus(Block.Status.PENDING);

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "NON_COMMITTED_BLOCK_STATUS");
    }

    @Test
    void validateFullChain_rejectsInvalidGenesisPreviousHash() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.genesis.setPreviousHash(hex64(9999));

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_GENESIS_PREVIOUS_HASH");
    }

    @Test
    void validateFullChain_rejectsInvalidGenesisHeight() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.genesis.setBlockHeight(1);

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_GENESIS_HEIGHT");
    }

    @Test
    void validateFullChain_rejectsNonContiguousHeight() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.block2.setBlockHeight(4);

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "NON_CONTIGUOUS_BLOCK_HEIGHT");
    }

    @Test
    void validateFullChain_rejectsBrokenPreviousHashLink() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.block2.setPreviousHash(hex64(7001));

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "BROKEN_PREVIOUS_HASH_LINK");
    }

    @Test
    void validateFullChain_rejectsBlockTimestampRegression() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.block2.setTimestamp(fixture.block1.getTimestamp().minusSeconds(1));

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "BLOCK_TIMESTAMP_REGRESSION");
    }

    @Test
    void validateFullChain_rejectsInvalidBlockHashFormat() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.block1.setBlockHash("not-a-hash");

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_BLOCK_HASH_FORMAT");
    }

    @Test
    void validateFullChain_rejectsInvalidPreviousHashFormat() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.block1.setPreviousHash("bad-prev");

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_PREVIOUS_HASH_FORMAT");
    }

    @Test
    void validateFullChain_rejectsMissingReferencedTransaction() {
        arrangeValidFullChain();
        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(2))
                .thenReturn(List.of(blockTransaction(5002, 2, 999)));
        when(transactionRepository.findById(999)).thenReturn(Optional.empty());

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "TRANSACTION_REFERENCE_NOT_FOUND");
    }

    @Test
    void validateFullChain_rejectsTransactionCountMismatch() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.block1.setTransactionCount(2);

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "TRANSACTION_COUNT_MISMATCH");
    }

    @Test
    void validateFullChain_rejectsUnconfirmedTransactionInBlock() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.aliceToBob.setStatus(Transaction.Status.PENDING);

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "UNCONFIRMED_TRANSACTION_IN_BLOCK");
    }

    @Test
    void validateFullChain_rejectsTransactionBlockLinkMismatch() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.aliceToBob.setBlockId(999);

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "TRANSACTION_BLOCK_LINK_MISMATCH");
    }

    @Test
    void validateFullChain_rejectsDuplicateTransactionHashAcrossChain() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.bobToAlice.setTransactionHash(fixture.aliceToBob.getTransactionHash());

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "DUPLICATE_TRANSACTION_HASH_ACROSS_CHAIN");
    }

    @Test
    void validateFullChain_rejectsInvalidTransactionHashFormat() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.aliceToBob.setTransactionHash("bad-hash");

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_TRANSACTION_HASH_FORMAT");
    }

    @Test
    void validateFullChain_rejectsInvalidTransactionHashContentMismatch() {
        ChainFixture fixture = arrangeValidFullChain();
        when(transactionService.generateTransactionHash(
                fixture.aliceToBob.getSenderAddress(),
                fixture.aliceToBob.getReceiverAddress(),
                fixture.aliceToBob.getAmount(),
                fixture.aliceToBob.getFee(),
                fixture.aliceToBob.getNonce(),
                fixture.aliceToBob.getTimestamp()
        )).thenReturn(hex64(9001));

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_TRANSACTION_HASH");
    }

    @Test
    void validateFullChain_rejectsTransactionAfterBlockTimestamp() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.aliceToBob.setTimestamp(fixture.block1.getTimestamp().plusSeconds(1));

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "TRANSACTION_AFTER_BLOCK_TIMESTAMP");
    }

    @Test
    void validateFullChain_rejectsNonMonotonicTransactionTimestamps() {
        ChainFixture fixture = arrangeValidFullChain();

        fixture.block1.setTransactionCount(2);
        fixture.block2.setTransactionCount(0);
        fixture.bobToAlice.setBlockId(2);
        fixture.bobToAlice.setTimestamp(fixture.aliceToBob.getTimestamp().minusSeconds(1));

        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(2))
                .thenReturn(List.of(
                        blockTransaction(5002, 2, 11),
                        blockTransaction(5003, 2, 12)
                ));
        lenient().when(blockTransactionRepository.findByBlockIdOrderByIdAsc(3)).thenReturn(List.of());

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "NON_MONOTONIC_TRANSACTION_TIMESTAMPS");
    }

    @Test
    void validateFullChain_rejectsInvalidReceiverAddress() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.aliceToBob.setReceiverAddress(" ");

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_RECEIVER_ADDRESS");
    }

    @Test
    void validateFullChain_rejectsInvalidSenderAddress() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.aliceToBob.setSenderAddress(" ");

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_SENDER_ADDRESS");
    }

    @Test
    void validateFullChain_rejectsSelfTransfer() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.aliceToBob.setReceiverAddress(fixture.aliceToBob.getSenderAddress());

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "SELF_TRANSFER_NOT_ALLOWED");
    }

    @Test
    void validateFullChain_rejectsInvalidAmount() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.aliceToBob.setAmount(BigDecimal.ZERO);

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_AMOUNT");
    }

    @Test
    void validateFullChain_rejectsInvalidFee() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.aliceToBob.setFee(new BigDecimal("-0.01"));

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_FEE");
    }

    @Test
    void validateFullChain_rejectsInvalidNonce() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.aliceToBob.setNonce(-1);

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_NONCE");
    }

    @Test
    void validateFullChain_rejectsCoinbaseFee() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.mintToAlice.setFee(new BigDecimal("0.01"));

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INVALID_COINBASE_FEE");
    }

    @Test
    void validateFullChain_rejectsNonceReplayOrOutOfOrder() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.bobToAlice.setSenderAddress("alice");
        fixture.bobToAlice.setReceiverAddress("charlie");
        fixture.bobToAlice.setNonce(1);
        fixture.bobToAlice.setAmount(new BigDecimal("2.00000000"));
        fixture.bobToAlice.setFee(new BigDecimal("0.50000000"));

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "NONCE_REPLAY_OR_OUT_OF_ORDER");
    }

    @Test
    void validateFullChain_rejectsInsufficientSenderBalance() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.aliceToBob.setAmount(new BigDecimal("1000.00000000"));

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "INSUFFICIENT_SENDER_BALANCE");
    }

    @Test
    void validateFullChain_rejectsBlockHashMismatch() {
        ChainFixture fixture = arrangeValidFullChain();
        when(blockService.generateBlockHash(
                fixture.block1.getBlockHeight(),
                fixture.block1.getPreviousHash(),
                fixture.block1.getTimestamp(),
                List.of(fixture.aliceToBob.getTransactionHash())
        )).thenReturn(hex64(123456));

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertFailure(report, "BLOCK_HASH_MISMATCH");
    }

    @Test
    void validateBlock_missingBlock_returnsNotFoundFailure() {
        when(blockRepository.findByBlockHeight(99)).thenReturn(Optional.empty());

        ChainValidationReportDTO report = chainValidationService.validateBlock(99);

        assertFailure(report, "BLOCK_NOT_FOUND");
    }

    @Test
    void validateBlock_missingPreviousBlock_fails() {
        ChainFixture fixture = arrangeValidFullChain();
        when(blockRepository.findByBlockHeight(1)).thenReturn(Optional.of(fixture.block1));
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.empty());

        ChainValidationReportDTO report = chainValidationService.validateBlock(1);

        assertFailure(report, "PREVIOUS_BLOCK_NOT_FOUND");
    }

    @Test
    void validateBlock_validGenesisBlock_passes() {
        ChainFixture fixture = arrangeValidFullChain();
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertTrue(report.isValid());
        assertEquals(1, report.getCheckedBlocks());
        assertEquals(1, report.getCheckedTransactions());
        assertNull(report.getFailure());
    }

    @Test
    void validateFullChain_emptyChain_passes() {
        when(blockRepository.findAllByOrderByBlockHeightAsc()).thenReturn(List.of());

        ChainValidationReportDTO report = chainValidationService.validateFullChain();

        assertTrue(report.isValid());
        assertEquals(0, report.getCheckedBlocks());
        assertEquals(0, report.getCheckedTransactions());
        assertNull(report.getFailure());
    }

    @Test
    void validateBlock_rejectsMissingBlockId() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.genesis.setId(null);
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "MISSING_BLOCK_ID");
    }

    @Test
    void validateBlock_rejectsMissingBlockHeight() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.genesis.setBlockHeight(null);
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "MISSING_BLOCK_HEIGHT");
    }

    @Test
    void validateBlock_rejectsMissingBlockTimestamp() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.genesis.setTimestamp(null);
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "BLOCK_TIMESTAMP_MISSING");
    }

    @Test
    void validateBlock_rejectsFutureBlockTimestamp() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.genesis.setTimestamp(LocalDateTime.now().plusMinutes(30));
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "BLOCK_TIMESTAMP_IN_FUTURE");
    }

    @Test
    void validateBlock_rejectsMissingTransactionCount() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.genesis.setTransactionCount(null);
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "MISSING_TRANSACTION_COUNT");
    }

    @Test
    void validateBlock_rejectsMissingTransactionId() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.mintToAlice.setId(null);
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "TRANSACTION_ID_MISSING");
    }

    @Test
    void validateBlock_rejectsDuplicateTransactionIdWithinBlock() {
        ChainFixture fixture = arrangeValidFullChain();
        Transaction duplicateIdTx = transaction(
                10,
                null,
                "bob",
                new BigDecimal("1.00000000"),
                BigDecimal.ZERO,
                0,
                fixture.genesis.getTimestamp(),
                hex64(3001),
                1
        );
        fixture.genesis.setTransactionCount(2);

        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));
        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(1))
                .thenReturn(List.of(
                        blockTransaction(5001, 1, 10),
                        blockTransaction(5004, 1, 13)
                ));
        when(transactionRepository.findById(13)).thenReturn(Optional.of(duplicateIdTx));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "DUPLICATE_TRANSACTION_ID_IN_BLOCK");
    }

    @Test
    void validateBlock_rejectsDuplicateTransactionHashWithinBlock() {
        ChainFixture fixture = arrangeValidFullChain();
        Transaction duplicateHashTx = transaction(
                13,
                null,
                "bob",
                new BigDecimal("1.00000000"),
                BigDecimal.ZERO,
                0,
                fixture.genesis.getTimestamp(),
                fixture.mintToAlice.getTransactionHash(),
                1
        );
        fixture.genesis.setTransactionCount(2);

        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));
        when(blockTransactionRepository.findByBlockIdOrderByIdAsc(1))
                .thenReturn(List.of(
                        blockTransaction(5001, 1, 10),
                        blockTransaction(5004, 1, 13)
                ));
        when(transactionRepository.findById(13)).thenReturn(Optional.of(duplicateHashTx));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "DUPLICATE_TRANSACTION_HASH_IN_BLOCK");
    }

    @Test
    void validateBlock_rejectsMissingTransactionTimestamp() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.mintToAlice.setTimestamp(null);
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "TRANSACTION_TIMESTAMP_MISSING");
    }

    @Test
    void validateBlock_rejectsFutureTransactionTimestamp() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.mintToAlice.setTimestamp(LocalDateTime.now().plusMinutes(30));
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "TRANSACTION_TIMESTAMP_IN_FUTURE");
    }

    @Test
    void validateBlock_rejectsNullReceiverAddress() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.mintToAlice.setReceiverAddress(null);
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "INVALID_RECEIVER_ADDRESS");
    }

    @Test
    void validateBlock_rejectsInvalidAmount() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.mintToAlice.setAmount(BigDecimal.ZERO);
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "INVALID_AMOUNT");
    }

    @Test
    void validateBlock_rejectsInvalidFee() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.mintToAlice.setFee(new BigDecimal("-1.00"));
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "INVALID_FEE");
    }

    @Test
    void validateBlock_rejectsInvalidNonce() {
        ChainFixture fixture = arrangeValidFullChain();
        fixture.mintToAlice.setNonce(-1);
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "INVALID_NONCE");
    }

    @Test
    void validateBlock_rejectsTransactionHashMismatch() {
        ChainFixture fixture = arrangeValidFullChain();
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));
        when(transactionService.generateTransactionHash(
                fixture.mintToAlice.getSenderAddress(),
                fixture.mintToAlice.getReceiverAddress(),
                fixture.mintToAlice.getAmount(),
                fixture.mintToAlice.getFee(),
                fixture.mintToAlice.getNonce(),
                fixture.mintToAlice.getTimestamp()
        )).thenReturn(hex64(8888));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "INVALID_TRANSACTION_HASH");
    }

    @Test
    void validateBlock_rejectsBlockHashMismatch() {
        ChainFixture fixture = arrangeValidFullChain();
        when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(fixture.genesis));
        when(blockService.generateBlockHash(
                fixture.genesis.getBlockHeight(),
                fixture.genesis.getPreviousHash(),
                fixture.genesis.getTimestamp(),
                List.of(fixture.mintToAlice.getTransactionHash())
        )).thenReturn(hex64(99999));

        ChainValidationReportDTO report = chainValidationService.validateBlock(0);

        assertFailure(report, "BLOCK_HASH_MISMATCH");
    }

    private void assertFailure(ChainValidationReportDTO report, String expectedCode) {
        assertFalse(report.isValid());
        assertNotNull(report.getFailure());
        assertEquals(expectedCode, report.getFailure().getCode());
    }

    private ChainFixture arrangeValidFullChain() {
        LocalDateTime genesisTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime block1Time = LocalDateTime.of(2026, 1, 1, 0, 2, 0);
        LocalDateTime block2Time = LocalDateTime.of(2026, 1, 1, 0, 4, 0);

        Block genesis = block(1, 0, ZERO_HASH, hex64(1000), genesisTime, 1);
        Block block1 = block(2, 1, genesis.getBlockHash(), hex64(1001), block1Time, 1);
        Block block2 = block(3, 2, block1.getBlockHash(), hex64(1002), block2Time, 1);

        Transaction mintToAlice = transaction(
                10,
                null,
                "alice",
                new BigDecimal("100.00000000"),
                BigDecimal.ZERO,
                0,
                LocalDateTime.of(2026, 1, 1, 0, 0, 0),
                hex64(2000),
                1
        );
        Transaction aliceToBob = transaction(
                11,
                "alice",
                "bob",
                new BigDecimal("10.00000000"),
                new BigDecimal("1.00000000"),
                1,
                LocalDateTime.of(2026, 1, 1, 0, 2, 0),
                hex64(2001),
                2
        );
        Transaction bobToAlice = transaction(
                12,
                "bob",
                "alice",
                new BigDecimal("2.00000000"),
                new BigDecimal("0.50000000"),
                1,
                LocalDateTime.of(2026, 1, 1, 0, 3, 0),
                hex64(2002),
                3
        );

        lenient().when(blockRepository.findAllByOrderByBlockHeightAsc())
                .thenReturn(List.of(genesis, block1, block2));
        lenient().when(blockRepository.findByBlockHeight(0)).thenReturn(Optional.of(genesis));
        lenient().when(blockRepository.findByBlockHeight(1)).thenReturn(Optional.of(block1));
        lenient().when(blockRepository.findByBlockHeight(2)).thenReturn(Optional.of(block2));

        lenient().when(blockTransactionRepository.findByBlockIdOrderByIdAsc(1))
                .thenReturn(List.of(blockTransaction(5001, 1, 10)));
        lenient().when(blockTransactionRepository.findByBlockIdOrderByIdAsc(2))
                .thenReturn(List.of(blockTransaction(5002, 2, 11)));
        lenient().when(blockTransactionRepository.findByBlockIdOrderByIdAsc(3))
                .thenReturn(List.of(blockTransaction(5003, 3, 12)));

        lenient().when(transactionRepository.findById(10)).thenReturn(Optional.of(mintToAlice));
        lenient().when(transactionRepository.findById(11)).thenReturn(Optional.of(aliceToBob));
        lenient().when(transactionRepository.findById(12)).thenReturn(Optional.of(bobToAlice));

        lenient().when(transactionService.generateTransactionHash(
                mintToAlice.getSenderAddress(),
                mintToAlice.getReceiverAddress(),
                mintToAlice.getAmount(),
                mintToAlice.getFee(),
                mintToAlice.getNonce(),
                mintToAlice.getTimestamp()
        )).thenReturn(mintToAlice.getTransactionHash());
        lenient().when(transactionService.generateTransactionHash(
                aliceToBob.getSenderAddress(),
                aliceToBob.getReceiverAddress(),
                aliceToBob.getAmount(),
                aliceToBob.getFee(),
                aliceToBob.getNonce(),
                aliceToBob.getTimestamp()
        )).thenReturn(aliceToBob.getTransactionHash());
        lenient().when(transactionService.generateTransactionHash(
                bobToAlice.getSenderAddress(),
                bobToAlice.getReceiverAddress(),
                bobToAlice.getAmount(),
                bobToAlice.getFee(),
                bobToAlice.getNonce(),
                bobToAlice.getTimestamp()
        )).thenReturn(bobToAlice.getTransactionHash());

        lenient().when(blockService.generateBlockHash(
                genesis.getBlockHeight(),
                genesis.getPreviousHash(),
                genesis.getTimestamp(),
                List.of(mintToAlice.getTransactionHash())
        )).thenReturn(genesis.getBlockHash());
        lenient().when(blockService.generateBlockHash(
                block1.getBlockHeight(),
                block1.getPreviousHash(),
                block1.getTimestamp(),
                List.of(aliceToBob.getTransactionHash())
        )).thenReturn(block1.getBlockHash());
        lenient().when(blockService.generateBlockHash(
                block2.getBlockHeight(),
                block2.getPreviousHash(),
                block2.getTimestamp(),
                List.of(bobToAlice.getTransactionHash())
        )).thenReturn(block2.getBlockHash());

        return new ChainFixture(genesis, block1, block2, mintToAlice, aliceToBob, bobToAlice);
    }

    private Block block(
            int id,
            int height,
            String previousHash,
            String blockHash,
            LocalDateTime timestamp,
            int transactionCount) {
        Block block = new Block();
        block.setId(id);
        block.setBlockHeight(height);
        block.setPreviousHash(previousHash);
        block.setBlockHash(blockHash);
        block.setTimestamp(timestamp);
        block.setTransactionCount(transactionCount);
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
            String senderAddress,
            String receiverAddress,
            BigDecimal amount,
            BigDecimal fee,
            int nonce,
            LocalDateTime timestamp,
            String hash,
            int blockId) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setSenderAddress(senderAddress);
        tx.setReceiverAddress(receiverAddress);
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setNonce(nonce);
        tx.setTimestamp(timestamp);
        tx.setTransactionHash(hash);
        tx.setStatus(Transaction.Status.CONFIRMED);
        tx.setBlockId(blockId);
        return tx;
    }

    private String hex64(int value) {
        return String.format("%064x", value);
    }

    private static final class ChainFixture {
        private final Block genesis;
        private final Block block1;
        private final Block block2;
        private final Transaction mintToAlice;
        private final Transaction aliceToBob;
        private final Transaction bobToAlice;

        private ChainFixture(
                Block genesis,
                Block block1,
                Block block2,
                Transaction mintToAlice,
                Transaction aliceToBob,
                Transaction bobToAlice) {
            this.genesis = genesis;
            this.block1 = block1;
            this.block2 = block2;
            this.mintToAlice = mintToAlice;
            this.aliceToBob = aliceToBob;
            this.bobToAlice = bobToAlice;
        }
    }
}
