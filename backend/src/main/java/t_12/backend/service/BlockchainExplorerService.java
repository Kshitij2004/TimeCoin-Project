package t_12.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import t_12.backend.api.blockchain.dto.BlockDetailDTO;
import t_12.backend.api.blockchain.dto.ChainStatusDTO;
import t_12.backend.api.blockchain.dto.ExplorerTransactionDTO;
import t_12.backend.entity.Block;
import t_12.backend.entity.Transaction;
import t_12.backend.repository.BlockRepository;
import t_12.backend.repository.TransactionRepository;

/**
 * Read-only blockchain explorer queries for blocks and chain summary data.
 */
@Service
public class BlockchainExplorerService {

    private final BlockService blockService;
    private final BlockRepository blockRepository;
    private final TransactionRepository transactionRepository;

    public BlockchainExplorerService(
            BlockService blockService,
            BlockRepository blockRepository,
            TransactionRepository transactionRepository) {
        this.blockService = blockService;
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

    private BlockDetailDTO toBlockDetail(Block block) {
        List<ExplorerTransactionDTO> transactions = blockService.getBlockTransactions(block.getId())
                .stream()
                .map(ExplorerTransactionDTO::new)
                .toList();

        return new BlockDetailDTO(block, transactions);
    }
}
