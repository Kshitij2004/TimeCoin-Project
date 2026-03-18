package t_12.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import t_12.backend.entity.Block;
import t_12.backend.entity.BlockTransaction;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.BlockRepository;
import t_12.backend.repository.BlockTransactionRepository;
import t_12.backend.repository.TransactionRepository;

/**
 * Service class for handling block-related business logic. Manages block
 * creation, genesis block initialization, SHA-256 hash generation over
 * block contents, chain linking via previousHash, and transaction-to-block
 * association through the block_transactions join table.
 */
@Service
public class BlockService {

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
     * The genesis block has no previous hash, no validator, and zero
     * transactions. Its hash is deterministic so every node produces
     * the same genesis block.
     *
     * @return the saved genesis Block, or the existing one if already created
     */
    public Block createGenesisBlock() {
        // prevent duplicate genesis block
        Optional<Block> existing = blockRepository.findByBlockHeight(0);
        if (existing.isPresent()) {
            return existing.get();
        }

        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 4, 0, 0, 0);
        String hash = generateBlockHash(0, null, null, timestamp, List.of());

        Block genesis = new Block();
        genesis.setBlockHeight(0);
        genesis.setPreviousHash(null);
        genesis.setBlockHash(hash);
        genesis.setValidatorAddress(null);
        genesis.setTimestamp(timestamp);
        genesis.setTransactionCount(0);
        genesis.setStatus(Block.Status.COMMITTED);

        return blockRepository.save(genesis);
    }

    /**
     * Creates a new block linked to the current chain tip. Computes the
     * block hash over the canonical fields, persists the block, and
     * associates the given transactions via the block_transactions join
     * table. Each transaction's status is updated to CONFIRMED and its
     * blockId is set.
     *
     * @param validatorAddress the wallet address of the proposing validator
     * @param transactions     the list of transactions to include in the block
     * @return the saved Block entity
     * @throws ResourceNotFoundException  if the chain is empty (no genesis block)
     * @throws DuplicateResourceException if a block at the computed height already exists
     */
    public Block createBlock(String validatorAddress, List<Transaction> transactions) {
        // get the latest block to link to
        Block previousBlock = getLatestBlock();

        Integer newHeight = previousBlock.getBlockHeight() + 1;

        // prevent duplicate height (shouldn't happen with unique constraint,
        // but checking here gives a clearer error message)
        if (blockRepository.existsByBlockHeight(newHeight)) {
            throw new DuplicateResourceException(
                    "Block at height " + newHeight + " already exists");
        }

        LocalDateTime timestamp = LocalDateTime.now();

        // collect transaction hashes for block hash computation
        List<String> transactionHashes = transactions.stream()
                .map(Transaction::getTransactionHash)
                .collect(Collectors.toList());

        String blockHash = generateBlockHash(
                newHeight, previousBlock.getBlockHash(),
                validatorAddress, timestamp, transactionHashes);

        // build and save the block
        Block block = new Block();
        block.setBlockHeight(newHeight);
        block.setPreviousHash(previousBlock.getBlockHash());
        block.setBlockHash(blockHash);
        block.setValidatorAddress(validatorAddress);
        block.setTimestamp(timestamp);
        block.setTransactionCount(transactions.size());
        block.setStatus(Block.Status.COMMITTED);

        Block savedBlock = blockRepository.save(block);

        // associate each transaction with this block
        for (Transaction tx : transactions) {
            // update the transaction itself
            tx.setBlockId(savedBlock.getId());
            tx.setStatus(Transaction.Status.CONFIRMED);
            transactionRepository.save(tx);

            // create the join table entry
            BlockTransaction blockTx = new BlockTransaction();
            blockTx.setBlockId(savedBlock.getId());
            blockTx.setTransactionId(tx.getId());
            blockTransactionRepository.save(blockTx);
        }

        return savedBlock;
    }

    /**
     * Retrieves a block by its height.
     *
     * @param blockHeight the height to look up
     * @return the Block entity
     * @throws ResourceNotFoundException if no block exists at that height
     */
    public Block findByHeight(Integer blockHeight) {
        return blockRepository.findByBlockHeight(blockHeight)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Block not found at height: " + blockHeight
                ));
    }

    /**
     * Retrieves a block by its hash.
     *
     * @param blockHash the hash to look up
     * @return the Block entity
     * @throws ResourceNotFoundException if no block exists with that hash
     */
    public Block findByHash(String blockHash) {
        return blockRepository.findByBlockHash(blockHash)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Block not found with hash: " + blockHash
                ));
    }

    /**
     * Retrieves a block by its ID.
     *
     * @param id the block ID
     * @return the Block entity
     * @throws ResourceNotFoundException if no block exists with that ID
     */
    public Block findById(Integer id) {
        return blockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Block not found with id: " + id
                ));
    }

    /**
     * Retrieves the latest block in the chain (highest block_height).
     *
     * @return the latest Block entity
     * @throws ResourceNotFoundException if the chain is empty
     */
    public Block getLatestBlock() {
        return blockRepository.findTopByOrderByBlockHeightDesc()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chain is empty. Create genesis block first."
                ));
    }

    /**
     * Returns the current chain height (the block_height of the latest block).
     *
     * @return the chain height, or -1 if the chain is empty
     */
    public Integer getChainHeight() {
        Optional<Block> latest = blockRepository.findTopByOrderByBlockHeightDesc();
        return latest.map(Block::getBlockHeight).orElse(-1);
    }

    /**
     * Retrieves all blocks in the chain.
     *
     * @return list of all blocks
     */
    public List<Block> findAll() {
        return blockRepository.findAll();
    }

    /**
     * Retrieves all blocks with a given status.
     *
     * @param status the status to filter by
     * @return list of blocks with the given status
     */
    public List<Block> findByStatus(Block.Status status) {
        return blockRepository.findByStatus(status);
    }

    /**
     * Retrieves all transaction IDs associated with a block through
     * the block_transactions join table.
     *
     * @param blockId the block ID
     * @return list of transaction IDs in the block
     */
    public List<Integer> findTransactionIdsInBlock(Integer blockId) {
        return blockTransactionRepository.findByBlockId(blockId)
                .stream()
                .map(BlockTransaction::getTransactionId)
                .collect(Collectors.toList());
    }

    /**
     * Generates a deterministic SHA-256 hash over the canonical block
     * fields. The canonical format is a pipe-delimited string:
     * "height|previousHash|validatorAddress|timestamp|txHash1,txHash2,..."
     *
     * Including transaction hashes in the block hash means any change
     * to any transaction in the block changes the block hash, making
     * tampering detectable. This is a simplified version of a Merkle root.
     *
     * @param blockHeight       the block's position in the chain
     * @param previousHash      the hash of the preceding block
     * @param validatorAddress  the proposing validator's address
     * @param timestamp         the block creation timestamp
     * @param transactionHashes the hashes of all included transactions
     * @return the hex-encoded SHA-256 hash string
     */
    public String generateBlockHash(Integer blockHeight, String previousHash,
                                     String validatorAddress, LocalDateTime timestamp,
                                     List<String> transactionHashes) {

        // join all tx hashes with commas. empty list produces empty string.
        String txDigest = String.join(",", transactionHashes);

        String canonical = blockHeight
                + "|" + (previousHash != null ? previousHash : "null")
                + "|" + (validatorAddress != null ? validatorAddress : "null")
                + "|" + timestamp.toString()
                + "|" + txDigest;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Converts a byte array to a lowercase hex string.
     *
     * @param bytes the byte array to convert
     * @return the hex-encoded string
     */
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