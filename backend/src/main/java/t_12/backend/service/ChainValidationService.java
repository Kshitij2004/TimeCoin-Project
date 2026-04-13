package t_12.backend.service;

import java.util.List;
import java.util.stream.Collectors;

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

        int checkedBlocks = 0;
        int checkedTransactions = 0;
        Block previousBlock = null;

        for (Block block : blocks) {
            List<Transaction> transactions = loadOrderedBlockTransactions(block.getId());
            checkedTransactions += transactions.size();

            ChainValidationFailureDTO linkFailure = validatePreviousHashLink(block, previousBlock);
            if (linkFailure != null) {
                return ChainValidationReportDTO.failed(
                        checkedBlocks + 1,
                        checkedTransactions,
                        linkFailure
                );
            }

            ChainValidationFailureDTO transactionFailure = validateTransactionHashes(block, transactions);
            if (transactionFailure != null) {
                return ChainValidationReportDTO.failed(
                        checkedBlocks + 1,
                        checkedTransactions,
                        transactionFailure
                );
            }

            ChainValidationFailureDTO blockFailure = validateSingleBlock(block, transactions);
            if (blockFailure != null) {
                return ChainValidationReportDTO.failed(
                        checkedBlocks + 1,
                        checkedTransactions,
                        blockFailure
                );
            }

            checkedBlocks++;
            previousBlock = block;
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

        List<Transaction> transactions = loadOrderedBlockTransactions(block.getId());

        ChainValidationFailureDTO transactionFailure = validateTransactionHashes(block, transactions);
        if (transactionFailure != null) {
            return ChainValidationReportDTO.failed(1, transactions.size(), transactionFailure);
        }

        ChainValidationFailureDTO blockFailure = validateSingleBlock(block, transactions);
        if (blockFailure != null) {
            return ChainValidationReportDTO.failed(1, transactions.size(), blockFailure);
        }

        return ChainValidationReportDTO.passed(1, transactions.size());
    }

    private List<Transaction> loadOrderedBlockTransactions(Integer blockId) {
        List<BlockTransaction> joins = blockTransactionRepository.findByBlockIdOrderByIdAsc(blockId);
        return joins.stream()
                .map(join -> transactionRepository.findById(join.getTransactionId()).orElse(null))
                .filter(tx -> tx != null)
                .collect(Collectors.toList());
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
            return new ChainValidationFailureDTO(
                    block.getBlockHeight(),
                    block.getBlockHash(),
                    "BLOCK_HASH_MISMATCH",
                    "Block hash does not match block contents",
                    null,
                    null
            );
        }

        return null;
    }

    private ChainValidationFailureDTO validatePreviousHashLink(Block block, Block previousBlock) {
        if (previousBlock == null) {
            return null;
        }

        if (!previousBlock.getBlockHash().equals(block.getPreviousHash())) {
            return new ChainValidationFailureDTO(
                    block.getBlockHeight(),
                    block.getBlockHash(),
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
                return new ChainValidationFailureDTO(
                        block.getBlockHeight(),
                        block.getBlockHash(),
                        "INVALID_TRANSACTION_HASH",
                        "Transaction hash does not match transaction contents",
                        tx.getId(),
                        tx.getTransactionHash()
                );
            }
        }

        return null;
    }
}
