package t_12.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import t_12.backend.api.blockchain.dto.ChainValidationFailureDTO;
import t_12.backend.api.blockchain.dto.ChainValidationReportDTO;
import t_12.backend.entity.Block;
import t_12.backend.entity.BlockTransaction;
import t_12.backend.entity.Transaction;
import t_12.backend.repository.BlockRepository;
import t_12.backend.repository.BlockTransactionRepository;
import t_12.backend.repository.TransactionRepository;

/**
 * Validates chain integrity by checking block hash integrity, previous hash
 * links, and transaction hash integrity.
 */
@Service
public class ChainValidationService {

    private static final int GENESIS_BLOCK_HEIGHT = 0;
    private static final String GENESIS_PREVIOUS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";
    private static final int MAX_FUTURE_CLOCK_SKEW_MINUTES = 5;
    private static final Pattern HEX_64_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");

    private final BlockRepository blockRepository;
    private final BlockTransactionRepository blockTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final BlockService blockService;
    private final TransactionService transactionService;

    public ChainValidationService(
            BlockRepository blockRepository,
            BlockTransactionRepository blockTransactionRepository,
            TransactionRepository transactionRepository,
            BlockService blockService,
            TransactionService transactionService) {
        this.blockRepository = blockRepository;
        this.blockTransactionRepository = blockTransactionRepository;
        this.transactionRepository = transactionRepository;
        this.blockService = blockService;
        this.transactionService = transactionService;
    }

    /**
     * Validates all blocks in height order and returns the first failure, if
     * any.
     */
    public ChainValidationReportDTO validateFullChain() {
        List<Block> blocks = blockRepository.findAllByOrderByBlockHeightAsc();
        if (blocks.isEmpty()) {
            return ChainValidationReportDTO.passed(0, 0);
        }

        ValidationState state = new ValidationState();
        int checkedBlocks = 0;
        int checkedTransactions = 0;
        Block previousBlock = null;

        for (Block block : blocks) {
            ChainValidationFailureDTO blockFailure = validateBlockMetadata(block, previousBlock, state, checkedBlocks);
            if (blockFailure != null) {
                return ChainValidationReportDTO.failed(checkedBlocks + 1, checkedTransactions, blockFailure);
            }

            LoadTransactionsResult loadResult = loadOrderedBlockTransactions(block);
            if (loadResult.failure() != null) {
                return ChainValidationReportDTO.failed(checkedBlocks + 1, checkedTransactions, loadResult.failure());
            }

            List<Transaction> transactions = loadResult.transactions();
            checkedTransactions += transactions.size();

            ChainValidationFailureDTO countFailure = validateTransactionCount(block, transactions.size());
            if (countFailure != null) {
                return ChainValidationReportDTO.failed(checkedBlocks + 1, checkedTransactions, countFailure);
            }

            ChainValidationFailureDTO structureFailure = validateTransactionStructure(block, transactions, state, true);
            if (structureFailure != null) {
                return ChainValidationReportDTO.failed(checkedBlocks + 1, checkedTransactions, structureFailure);
            }

            ChainValidationFailureDTO semanticFailure = validateTransactionSemantics(
                    block,
                    transactions,
                    state,
                    true
            );
            if (semanticFailure != null) {
                return ChainValidationReportDTO.failed(checkedBlocks + 1, checkedTransactions, semanticFailure);
            }

            ChainValidationFailureDTO transactionFailure = validateTransactionHashes(block, transactions);
            if (transactionFailure != null) {
                return ChainValidationReportDTO.failed(
                        checkedBlocks + 1,
                        checkedTransactions,
                        transactionFailure
                );
            }

            ChainValidationFailureDTO blockHashFailure = validateSingleBlock(block, transactions);
            if (blockHashFailure != null) {
                return ChainValidationReportDTO.failed(
                        checkedBlocks + 1,
                        checkedTransactions,
                        blockHashFailure
                );
            }

            checkedBlocks++;
            previousBlock = block;
            state.previousBlockTimestamp = block.getTimestamp();
        }

        return ChainValidationReportDTO.passed(checkedBlocks, checkedTransactions);
    }

    /**
     * Validates a block's own integrity (transaction hashes and block hash)
     * without checking previous hash linkage.
     */
    public ChainValidationReportDTO validateBlock(Integer blockHeight) {
        Block block = blockRepository.findByBlockHeight(blockHeight).orElse(null);
        if (block == null) {
            ChainValidationFailureDTO failure = new ChainValidationFailureDTO(
                    blockHeight,
                    null,
                    "BLOCK_NOT_FOUND",
                    "Block not found at height " + blockHeight,
                    null,
                    null
            );
            return ChainValidationReportDTO.failed(0, 0, failure);
        }

        ValidationState state = new ValidationState();
        Block previousBlock = null;
        if (block.getBlockHeight() != null && block.getBlockHeight() > GENESIS_BLOCK_HEIGHT) {
            previousBlock = blockRepository.findByBlockHeight(block.getBlockHeight() - 1).orElse(null);
            if (previousBlock == null) {
                ChainValidationFailureDTO failure = new ChainValidationFailureDTO(
                        block.getBlockHeight(),
                        block.getBlockHash(),
                        "PREVIOUS_BLOCK_NOT_FOUND",
                        "Expected previous block at height " + (block.getBlockHeight() - 1),
                        null,
                        null
                );
                return ChainValidationReportDTO.failed(0, 0, failure);
            }
            state.previousBlockTimestamp = previousBlock.getTimestamp();
        }

        ChainValidationFailureDTO blockFailure = validateBlockMetadata(
                block,
                previousBlock,
                state,
                block.getBlockHeight() == null ? 0 : block.getBlockHeight()
        );
        if (blockFailure != null) {
            return ChainValidationReportDTO.failed(1, 0, blockFailure);
        }

        LoadTransactionsResult loadResult = loadOrderedBlockTransactions(block);
        if (loadResult.failure() != null) {
            return ChainValidationReportDTO.failed(1, 0, loadResult.failure());
        }

        List<Transaction> transactions = loadResult.transactions();

        ChainValidationFailureDTO countFailure = validateTransactionCount(block, transactions.size());
        if (countFailure != null) {
            return ChainValidationReportDTO.failed(1, transactions.size(), countFailure);
        }

        ChainValidationFailureDTO structureFailure = validateTransactionStructure(block, transactions, state, false);
        if (structureFailure != null) {
            return ChainValidationReportDTO.failed(1, transactions.size(), structureFailure);
        }

        ChainValidationFailureDTO semanticFailure = validateTransactionSemantics(block, transactions, state, false);
        if (semanticFailure != null) {
            return ChainValidationReportDTO.failed(1, transactions.size(), semanticFailure);
        }

        ChainValidationFailureDTO transactionFailure = validateTransactionHashes(block, transactions);
        if (transactionFailure != null) {
            return ChainValidationReportDTO.failed(1, transactions.size(), transactionFailure);
        }

        ChainValidationFailureDTO blockHashFailure = validateSingleBlock(block, transactions);
        if (blockHashFailure != null) {
            return ChainValidationReportDTO.failed(1, transactions.size(), blockHashFailure);
        }

        return ChainValidationReportDTO.passed(1, transactions.size());
    }

    private ChainValidationFailureDTO validateBlockMetadata(
            Block block,
            Block previousBlock,
            ValidationState state,
            int expectedHeight) {
        if (block.getStatus() != Block.Status.COMMITTED) {
            return failure(
                    block,
                    "NON_COMMITTED_BLOCK_STATUS",
                    "Encountered non-committed block in canonical chain",
                    null,
                    null
            );
        }
        if (block.getId() == null) {
            return failure(block, "MISSING_BLOCK_ID", "Block ID is missing", null, null);
        }
        if (block.getBlockHeight() == null) {
            return failure(block, "MISSING_BLOCK_HEIGHT", "Block height is missing", null, null);
        }
        if (!isValidHexHash(block.getBlockHash())) {
            return failure(
                    block,
                    "INVALID_BLOCK_HASH_FORMAT",
                    "Block hash must be a 64-character hex string",
                    null,
                    null
            );
        }
        if (block.getTimestamp() == null) {
            return failure(block, "BLOCK_TIMESTAMP_MISSING", "Block timestamp is missing", null, null);
        }
        LocalDateTime maxFuture = LocalDateTime.now().plusMinutes(MAX_FUTURE_CLOCK_SKEW_MINUTES);
        if (block.getTimestamp().isAfter(maxFuture)) {
            return failure(
                    block,
                    "BLOCK_TIMESTAMP_IN_FUTURE",
                    "Block timestamp is too far in the future",
                    null,
                    null
            );
        }
        if (state.previousBlockTimestamp != null && block.getTimestamp().isBefore(state.previousBlockTimestamp)) {
            return failure(
                    block,
                    "BLOCK_TIMESTAMP_REGRESSION",
                    "Block timestamp moved backwards relative to previous block",
                    null,
                    null
            );
        }

        if (previousBlock == null) {
            if (block.getBlockHeight() != GENESIS_BLOCK_HEIGHT) {
                return failure(
                        block,
                        "INVALID_GENESIS_HEIGHT",
                        "First block in chain must be genesis at height 0",
                        null,
                        null
                );
            }
            if (!GENESIS_PREVIOUS_HASH.equals(block.getPreviousHash())) {
                return failure(
                        block,
                        "INVALID_GENESIS_PREVIOUS_HASH",
                        "Genesis block must use canonical all-zero previousHash",
                        null,
                        null
                );
            }
            return null;
        }

        if (block.getBlockHeight() != expectedHeight) {
            return failure(
                    block,
                    "NON_CONTIGUOUS_BLOCK_HEIGHT",
                    "Expected block height " + expectedHeight + " but found " + block.getBlockHeight(),
                    null,
                    null
            );
        }
        if (!isValidHexHash(block.getPreviousHash())) {
            return failure(
                    block,
                    "INVALID_PREVIOUS_HASH_FORMAT",
                    "previousHash must be a 64-character hex string",
                    null,
                    null
            );
        }
        return validatePreviousHashLink(block, previousBlock);
    }

    private LoadTransactionsResult loadOrderedBlockTransactions(Block block) {
        List<BlockTransaction> joins = blockTransactionRepository.findByBlockIdOrderByIdAsc(block.getId());
        List<Transaction> ordered = new ArrayList<>();
        for (BlockTransaction join : joins) {
            Transaction tx = transactionRepository.findById(join.getTransactionId()).orElse(null);
            if (tx == null) {
                return LoadTransactionsResult.failure(
                        failure(
                                block,
                                "TRANSACTION_REFERENCE_NOT_FOUND",
                                "Block references missing transaction ID " + join.getTransactionId(),
                                join.getTransactionId(),
                                null
                        )
                );
            }
            ordered.add(tx);
        }
        return LoadTransactionsResult.success(ordered);
    }

    private ChainValidationFailureDTO validateTransactionCount(Block block, int resolvedCount) {
        if (block.getTransactionCount() == null) {
            return failure(
                    block,
                    "MISSING_TRANSACTION_COUNT",
                    "Block transactionCount is missing",
                    null,
                    null
            );
        }
        if (!block.getTransactionCount().equals(resolvedCount)) {
            return failure(
                    block,
                    "TRANSACTION_COUNT_MISMATCH",
                    "Block transactionCount does not match linked transaction rows",
                    null,
                    null
            );
        }
        return null;
    }

    private ChainValidationFailureDTO validateTransactionStructure(
            Block block,
            List<Transaction> transactions,
            ValidationState state,
            boolean enforceCrossBlockDuplicates) {
        Set<Integer> seenIdsInBlock = new HashSet<>();
        Set<String> seenHashesInBlock = new HashSet<>();

        for (Transaction tx : transactions) {
            if (tx.getId() == null) {
                return failure(block, "TRANSACTION_ID_MISSING", "Transaction ID is missing", null, tx.getTransactionHash());
            }
            if (!isValidHexHash(tx.getTransactionHash())) {
                return failure(
                        block,
                        "INVALID_TRANSACTION_HASH_FORMAT",
                        "Transaction hash must be a 64-character hex string",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (tx.getStatus() != Transaction.Status.CONFIRMED) {
                return failure(
                        block,
                        "UNCONFIRMED_TRANSACTION_IN_BLOCK",
                        "Block contains non-CONFIRMED transaction",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (tx.getBlockId() != null && !tx.getBlockId().equals(block.getId())) {
                return failure(
                        block,
                        "TRANSACTION_BLOCK_LINK_MISMATCH",
                        "Transaction blockId does not match enclosing block",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (!seenIdsInBlock.add(tx.getId())) {
                return failure(
                        block,
                        "DUPLICATE_TRANSACTION_ID_IN_BLOCK",
                        "Duplicate transaction ID found within block",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (!seenHashesInBlock.add(tx.getTransactionHash())) {
                return failure(
                        block,
                        "DUPLICATE_TRANSACTION_HASH_IN_BLOCK",
                        "Duplicate transaction hash found within block",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (enforceCrossBlockDuplicates) {
                if (!state.seenTransactionIds.add(tx.getId())) {
                    return failure(
                            block,
                            "DUPLICATE_TRANSACTION_ID_ACROSS_CHAIN",
                            "Transaction ID appears in multiple blocks",
                            tx.getId(),
                            tx.getTransactionHash()
                    );
                }
                if (!state.seenTransactionHashes.add(tx.getTransactionHash())) {
                    return failure(
                            block,
                            "DUPLICATE_TRANSACTION_HASH_ACROSS_CHAIN",
                            "Transaction hash appears in multiple blocks",
                            tx.getId(),
                            tx.getTransactionHash()
                    );
                }
            }
        }
        return null;
    }

    private ChainValidationFailureDTO validateTransactionSemantics(
            Block block,
            List<Transaction> transactions,
            ValidationState state,
            boolean enforceReplayAndBalances) {
        LocalDateTime maxFuture = LocalDateTime.now().plusMinutes(MAX_FUTURE_CLOCK_SKEW_MINUTES);
        LocalDateTime previousTxTimestamp = null;

        for (Transaction tx : transactions) {
            if (tx.getTimestamp() == null) {
                return failure(
                        block,
                        "TRANSACTION_TIMESTAMP_MISSING",
                        "Transaction timestamp is missing",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (tx.getTimestamp().isAfter(maxFuture)) {
                return failure(
                        block,
                        "TRANSACTION_TIMESTAMP_IN_FUTURE",
                        "Transaction timestamp is too far in the future",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (tx.getTimestamp().isAfter(block.getTimestamp())) {
                return failure(
                        block,
                        "TRANSACTION_AFTER_BLOCK_TIMESTAMP",
                        "Transaction timestamp cannot be after enclosing block timestamp",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (previousTxTimestamp != null && tx.getTimestamp().isBefore(previousTxTimestamp)) {
                return failure(
                        block,
                        "NON_MONOTONIC_TRANSACTION_TIMESTAMPS",
                        "Transactions are not ordered by non-decreasing timestamp",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            previousTxTimestamp = tx.getTimestamp();

            if (tx.getReceiverAddress() == null || tx.getReceiverAddress().isBlank()) {
                return failure(
                        block,
                        "INVALID_RECEIVER_ADDRESS",
                        "Receiver address is missing or blank",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (tx.getSenderAddress() != null && tx.getSenderAddress().isBlank()) {
                return failure(
                        block,
                        "INVALID_SENDER_ADDRESS",
                        "Sender address cannot be blank",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (tx.getSenderAddress() != null && tx.getSenderAddress().equals(tx.getReceiverAddress())) {
                return failure(
                        block,
                        "SELF_TRANSFER_NOT_ALLOWED",
                        "Sender and receiver addresses cannot be the same",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return failure(
                        block,
                        "INVALID_AMOUNT",
                        "Transaction amount must be greater than zero",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (tx.getFee() == null || tx.getFee().compareTo(BigDecimal.ZERO) < 0) {
                return failure(
                        block,
                        "INVALID_FEE",
                        "Transaction fee must be zero or greater",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
            if (tx.getNonce() == null || tx.getNonce() < 0) {
                return failure(
                        block,
                        "INVALID_NONCE",
                        "Transaction nonce must be non-negative",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }

            applyLedgerRules(block, tx, state, enforceReplayAndBalances);
            if (state.ledgerFailure != null) {
                return state.ledgerFailure;
            }
        }
        return null;
    }

    private void applyLedgerRules(
            Block block,
            Transaction tx,
            ValidationState state,
            boolean enforceReplayAndBalances) {
        String sender = tx.getSenderAddress();
        String receiver = tx.getReceiverAddress();
        BigDecimal amount = tx.getAmount();
        BigDecimal fee = tx.getFee();

        if (sender == null) {
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                state.ledgerFailure = failure(
                        block,
                        "INVALID_COINBASE_FEE",
                        "Coinbase-style transaction with null sender cannot include a fee",
                        tx.getId(),
                        tx.getTransactionHash()
                );
                return;
            }
            state.balances.merge(receiver, amount, BigDecimal::add);
            return;
        }

        if (enforceReplayAndBalances) {
            Integer highestNonce = state.highestNonceBySender.get(sender);
            if (highestNonce != null && tx.getNonce() <= highestNonce) {
                state.ledgerFailure = failure(
                        block,
                        "NONCE_REPLAY_OR_OUT_OF_ORDER",
                        "Nonce must strictly increase for each sender",
                        tx.getId(),
                        tx.getTransactionHash()
                );
                return;
            }
            state.highestNonceBySender.put(sender, tx.getNonce());

            BigDecimal available = state.balances.getOrDefault(sender, BigDecimal.ZERO);
            BigDecimal debit = amount.add(fee);
            if (available.compareTo(debit) < 0) {
                state.ledgerFailure = failure(
                        block,
                        "INSUFFICIENT_SENDER_BALANCE",
                        "Sender balance is insufficient for amount + fee at this chain position",
                        tx.getId(),
                        tx.getTransactionHash()
                );
                return;
            }

            state.balances.put(sender, available.subtract(debit));
        }

        state.balances.merge(receiver, amount, BigDecimal::add);
    }

    private ChainValidationFailureDTO validateSingleBlock(Block block, List<Transaction> transactions) {
        List<String> transactionHashes = transactions.stream()
                .map(Transaction::getTransactionHash)
                .toList();

        String expectedHash = blockService.generateBlockHash(
                block.getBlockHeight(),
                block.getPreviousHash(),
                block.getTimestamp(),
                transactionHashes
        );

        if (!expectedHash.equals(block.getBlockHash())) {
            return failure(
                    block,
                    "BLOCK_HASH_MISMATCH",
                    "Block hash does not match block contents",
                    null,
                    null
            );
        }

        return null;
    }

    private ChainValidationFailureDTO validatePreviousHashLink(Block block, Block previousBlock) {
        if (!previousBlock.getBlockHash().equals(block.getPreviousHash())) {
            return failure(
                    block,
                    "BROKEN_PREVIOUS_HASH_LINK",
                    "previousHash does not match prior blockHash",
                    null,
                    null
            );
        }

        return null;
    }

    private ChainValidationFailureDTO validateTransactionHashes(Block block, List<Transaction> transactions) {
        for (Transaction tx : transactions) {
            String expectedHash = transactionService.generateTransactionHash(
                    tx.getSenderAddress(),
                    tx.getReceiverAddress(),
                    tx.getAmount(),
                    tx.getFee(),
                    tx.getNonce(),
                    tx.getTimestamp()
            );

            if (!expectedHash.equals(tx.getTransactionHash())) {
                return failure(
                        block,
                        "INVALID_TRANSACTION_HASH",
                        "Transaction hash does not match transaction contents",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
        }

        return null;
    }

    private ChainValidationFailureDTO failure(
            Block block,
            String code,
            String message,
            Integer transactionId,
            String transactionHash) {
        return new ChainValidationFailureDTO(
                block.getBlockHeight(),
                block.getBlockHash(),
                code,
                message,
                transactionId,
                transactionHash
        );
    }

    private boolean isValidHexHash(String value) {
        return value != null && HEX_64_PATTERN.matcher(value).matches();
    }

    private static final class ValidationState {
        private final Map<String, BigDecimal> balances = new HashMap<>();
        private final Map<String, Integer> highestNonceBySender = new HashMap<>();
        private final Set<Integer> seenTransactionIds = new HashSet<>();
        private final Set<String> seenTransactionHashes = new HashSet<>();
        private LocalDateTime previousBlockTimestamp;
        private ChainValidationFailureDTO ledgerFailure;
    }

    private record LoadTransactionsResult(List<Transaction> transactions, ChainValidationFailureDTO failure) {
        private static LoadTransactionsResult success(List<Transaction> transactions) {
            return new LoadTransactionsResult(transactions, null);
        }

        private static LoadTransactionsResult failure(ChainValidationFailureDTO failure) {
            return new LoadTransactionsResult(List.of(), failure);
        }
    }
}
