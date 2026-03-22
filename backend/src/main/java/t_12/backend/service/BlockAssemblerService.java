package t_12.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import t_12.backend.entity.Block;
import t_12.backend.entity.Transaction;

/**
 * Does the block assembly process by pulling all PENDING transactions
 * from the mempool and packaging them into a new committed block.
 *
 * This service ties together MempoolService and BlockService into a single atomic operation.
 *
 * If any step fails, @Transactional ensures no partial state is persisted.
 */
@Service
public class BlockAssemblerService {

    private static final Logger log = LoggerFactory.getLogger(BlockAssemblerService.class);

    private final MempoolService mempoolService;
    private final BlockService blockService;

    public BlockAssemblerService(MempoolService mempoolService, BlockService blockService) {
        this.mempoolService = mempoolService;
        this.blockService = blockService;
    }

    /**
     * Assembles and commits a new block from all currently PENDING transactions.
     *
     * Steps:
     * 1. Fetch all PENDING transactions from the mempool
     * 2. Reject if mempool is empty — no point committing an empty block
     * 3. Delegate to BlockService to create the block, link it to the chain,
     *    create block_transaction join entries, and mark transactions CONFIRMED
     *
     * @param validatorAddress the wallet address of the validator triggering assembly
     *                         (nullable — null is acceptable for manual/system triggers)
     * @return the newly committed Block
     * @throws IllegalStateException if there are no pending transactions to assemble
     */
    @Transactional
    public Block assembleAndCommit(String validatorAddress) {
        // 1. Fetch all pending transactions from the mempool
        List<Transaction> pending = mempoolService.getPendingTransactions();

        log.info("Block assembly triggered. Found {} pending transaction(s).", pending.size());

        // 2. Reject if there is nothing to package
        if (pending.isEmpty()) {
            throw new IllegalStateException(
                    "No pending transactions in mempool. Block assembly aborted.");
        }

        // 3. Delegate block creation to BlockService.
        //    BlockService handles: chain linking, hash generation, join table entries,
        //    and marking each transaction as CONFIRMED.
        Block committed = blockService.createBlock(pending, validatorAddress);

        log.info("Block committed successfully. Height: {}, Hash: {}, Transactions: {}",
                committed.getBlockHeight(),
                committed.getBlockHash(),
                committed.getTransactionCount());

        return committed;
    }
}