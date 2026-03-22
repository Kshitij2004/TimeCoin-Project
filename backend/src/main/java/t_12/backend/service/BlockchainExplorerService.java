package t_12.backend.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import t_12.backend.api.blockchain.dto.BlockDetailDTO;
import t_12.backend.api.blockchain.dto.BlockListItemDTO;
import t_12.backend.api.blockchain.dto.BlockListPaginationDTO;
import t_12.backend.api.blockchain.dto.BlockListResponseDTO;
import t_12.backend.api.blockchain.dto.ChainStatusDTO;
import t_12.backend.api.blockchain.dto.ExplorerTransactionDTO;
import t_12.backend.entity.Block;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.ApiException;
import t_12.backend.repository.BlockRepository;
import t_12.backend.repository.TransactionRepository;

/**
 * Read-only blockchain explorer queries for blocks and chain summary data.
 */
@Service
public class BlockchainExplorerService {

    private final BlockService blockService;
    private final MempoolService mempoolService;
    private final BlockRepository blockRepository;
    private final TransactionRepository transactionRepository;

    public BlockchainExplorerService(
            BlockService blockService,
            MempoolService mempoolService,
            BlockRepository blockRepository,
            TransactionRepository transactionRepository) {
        this.blockService = blockService;
        this.mempoolService = mempoolService;
        this.blockRepository = blockRepository;
        this.transactionRepository = transactionRepository;
    }

    public BlockDetailDTO getBlockByHeight(Integer height) {
        Block block = blockService.findByHeight(height);
        return toBlockDetail(block);
    }

    public BlockDetailDTO getBlockByHash(String hash) {
        Block block = blockService.findByHash(hash);
        return toBlockDetail(block);
    }

    public BlockListResponseDTO getRecentBlocks(Integer page, Integer limit) {
        int resolvedPage = page == null ? 1 : page;
        int resolvedLimit = limit == null ? 20 : limit;

        if (resolvedPage < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "page must be a positive integer");
        }
        if (resolvedLimit < 1 || resolvedLimit > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "limit must be an integer between 1 and 100");
        }

        Page<Block> results = blockRepository.findAllByOrderByBlockHeightDesc(
                PageRequest.of(resolvedPage - 1, resolvedLimit)
        );

        List<BlockListItemDTO> items = results.getContent()
                .stream()
                .map(BlockListItemDTO::new)
                .toList();

        return new BlockListResponseDTO(
                items,
                new BlockListPaginationDTO(
                        resolvedPage,
                        resolvedLimit,
                        results.getTotalElements(),
                        results.getTotalPages()
                )
        );
    }

    public ChainStatusDTO getChainStatus() {
        Block latest = blockRepository.findTopByOrderByBlockHeightDesc().orElse(null);
        return new ChainStatusDTO(
                blockRepository.count(),
                blockRepository.countByStatus(Block.Status.COMMITTED),
                transactionRepository.countByStatus(Transaction.Status.PENDING),
                latest == null ? null : latest.getBlockHeight(),
                latest == null ? null : latest.getBlockHash(),
                latest == null ? null : latest.getTimestamp()
        );
    }

    public BlockDetailDTO minePendingTransactions(Integer limit, String validatorAddress) {
        try {
            int resolvedLimit = limit == null ? 100 : limit;
            if (resolvedLimit < 1 || resolvedLimit > 1000) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "limit must be an integer between 1 and 1000");
            }

            List<Transaction> pending = mempoolService.getPendingTransactions()
                    .stream()
                    .filter(tx -> tx != null && tx.getId() != null && tx.getTransactionHash() != null)
                    .sorted(Comparator
                            .comparing(Transaction::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Transaction::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .limit(resolvedLimit)
                    .toList();

            if (pending.isEmpty()) {
                throw new ApiException(HttpStatus.CONFLICT, "No pending transactions to mine");
            }

            if (blockRepository.count() == 0) {
                blockService.createGenesisBlock();
            }

            Block mined = blockService.createBlock(pending, validatorAddress);
            return toBlockDetail(mined);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Mining failed: " + rootCauseMessage(ex)
            );
        }
    }

    private BlockDetailDTO toBlockDetail(Block block) {
        List<ExplorerTransactionDTO> transactions = blockService.getBlockTransactions(block.getId())
                .stream()
                .map(ExplorerTransactionDTO::new)
                .toList();

        return new BlockDetailDTO(block, transactions);
    }

    private String rootCauseMessage(Exception ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
