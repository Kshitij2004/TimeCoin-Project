package t_12.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import t_12.backend.entity.Block;
import t_12.backend.entity.BlockTransaction;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.BlockRepository;
import t_12.backend.repository.BlockTransactionRepository;
import t_12.backend.repository.TransactionRepository;

/**
 * Service for block creation, chain linking, and block hash generation.
 * Handles genesis block creation, assembling new blocks from pending
 * transactions, and committing them to the chain with SHA-256 hashes
 * that include the previous block's hash for tamper detection.
 */
@Service
public class BlockService {

    private static final String GENESIS_PREVIOUS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private final BlockRepository blockRepository;
    private final BlockTransactionRepository blockTransactionRepository;
    private final TransactionRepository transactionRepository;

    public BlockService(BlockRepository blockRepository,
                        BlockTransactionRepository blockTransactionRepository,
                        TransactionRepository transactionRepository) {
        this.blockRepository = blockRepository;
        this.blockTransactionRepository = blockTransactionRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Creates the genesis block (height 0) if the chain is empty.
     * Uses a deterministic previousHash of all zeros. No-ops if a
     * genesis block already exists.
     *
     * @return the genesis block, or the existing one if already created
     */
    public Block createGenesisBlock() {
        if (blockRepository.existsByBlockHeight(0)) {
            return blockRepository.findByBlockHeight(0)
                    .orElseThrow(() -> new ResourceNotFoundException("Genesis block exists but could not be loaded"));
        }

        LocalDateTime timestamp = LocalDateTime.now();
        String blockHash = generateBlockHash(0, GENESIS_PREVIOUS_HASH, timestamp, List.of());

        Block genesis = new Block();
        genesis.setBlockHeight(0);
        genesis.setPreviousHash(GENESIS_PREVIOUS_HASH);
        genesis.setBlockHash(blockHash);
        genesis.setTimestamp(timestamp);
        genesis.setTransactionCount(0);
        genesis.setStatus(Block.Status.COMMITTED);
        genesis.setValidatorAddress(null);

        return blockRepository.save(genesis);
    }

    /**
     * Assembles and commits a new block from a list of transactions.
     * Links to the previous block via its hash, computes the new block
     * hash over the block contents (including transaction hashes), creates
     * join table entries, and marks transactions as CONFIRMED.
     *
     * @param transactions     the transactions to include in the block
     * @param validatorAddress the address of the validator creating the block (nullable)
     * @return the committed block
     * @throws IllegalStateException if the chain is empty (create genesis first)
     */
    @Transactional
    public Block createBlock(List<Transaction> transactions, String validatorAddress) {
        Block previousBlock = blockRepository.findTopByOrderByBlockHeightDesc()
                .orElseThrow(() -> new IllegalStateException(
                        "No blocks in chain. Create genesis block first."));

        int newHeight = previousBlock.getBlockHeight() + 1;
        String previousHash = previousBlock.getBlockHash();
        LocalDateTime timestamp = LocalDateTime.now();

        List<String> txHashes = transactions.stream()
                .map(Transaction::getTransactionHash)
                .collect(Collectors.toList());

        String blockHash = generateBlockHash(newHeight, previousHash, timestamp, txHashes);

        Block block = new Block();
        block.setBlockHeight(newHeight);
        block.setPreviousHash(previousHash);
        block.setBlockHash(blockHash);
        block.setTimestamp(timestamp);
        block.setTransactionCount(transactions.size());
        block.setStatus(Block.Status.COMMITTED);
        block.setValidatorAddress(validatorAddress);

        Block savedBlock = blockRepository.save(block);

        // create join table entries and confirm each transaction
        for (Transaction tx : transactions) {
            BlockTransaction bt = new BlockTransaction();
            bt.setBlockId(savedBlock.getId());
            bt.setTransactionId(tx.getId());
            blockTransactionRepository.save(bt);

            tx.setBlockId(savedBlock.getId());
            tx.setStatus(Transaction.Status.CONFIRMED);
            transactionRepository.save(tx);
        }

        return savedBlock;
    }

    /**
     * Generates a deterministic SHA-256 hash over the block's contents.
     * Canonical format: "height|previousHash|timestamp|txHash1,txHash2,..."
     *
     * Changing any field (including adding/removing/reordering transactions)
     * produces a completely different hash.
     */
    public String generateBlockHash(Integer blockHeight, String previousHash,
                                     LocalDateTime timestamp, List<String> transactionHashes) {

        String txHashesCombined = String.join(",", transactionHashes);

        String canonical = blockHeight
                + "|" + previousHash
                + "|" + timestamp.toString()
                + "|" + txHashesCombined;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    // --- lookup methods ---

    public Block findByHeight(Integer blockHeight) {
        return blockRepository.findByBlockHeight(blockHeight)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Block not found at height: " + blockHeight));
    }

    public Block findByHash(String blockHash) {
        return blockRepository.findByBlockHash(blockHash)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Block not found with hash: " + blockHash));
    }

    public Block findById(Integer id) {
        return blockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Block not found with id: " + id));
    }

    public Block getLatestBlock() {
        return blockRepository.findTopByOrderByBlockHeightDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No blocks in chain"));
    }

    public List<Block> findAll() {
        return blockRepository.findAll();
    }

    /**
     * Returns the transactions associated with a block via the join table.
     */
    public List<Transaction> getBlockTransactions(Integer blockId) {
        List<BlockTransaction> joins = blockTransactionRepository.findByBlockId(blockId);
        return joins.stream()
                .map(bt -> transactionRepository.findById(bt.getTransactionId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Transaction not found: " + bt.getTransactionId())))
                .collect(Collectors.toList());
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}