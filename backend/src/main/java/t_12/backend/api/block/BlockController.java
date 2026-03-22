package t_12.backend.api.block;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.entity.Block;
import t_12.backend.service.BlockAssemblerService;

/**
 * REST controller exposing block management endpoints.
 *
 * Provides a manual trigger for block assembly (packaging pending transactions
 * into a new block and committing them to the chain).
 *
 * Base path: /api/block
 */
@RestController
@RequestMapping("/api/block")
public class BlockController {

    private final BlockAssemblerService blockAssemblerService;

    public BlockController(BlockAssemblerService blockAssemblerService) {
        this.blockAssemblerService = blockAssemblerService;
    }

    /**
     * Pulls all PENDING transactions from the mempool, packages them into
     * a new block linked to the current chain tip, marks transactions as
     * CONFIRMED, and returns the committed block details.
     *
     * Returns 400 if the mempool is empty or the chain has no genesis block.
     *
     * @param validatorAddress optional wallet address of the triggering validator
     * @return 200 OK with block details on success, 400 on failure
     */
    @PostMapping("/assemble")
    public ResponseEntity<?> assembleBlock(
            @RequestParam(required = false) String validatorAddress) {
        try {
            Block block = blockAssemblerService.assembleAndCommit(validatorAddress);
            return ResponseEntity.ok(new BlockResponse(
                    block.getBlockHeight(),
                    block.getBlockHash(),
                    block.getPreviousHash(),
                    block.getTransactionCount(),
                    block.getStatus().name()
            ));
        } catch (IllegalStateException ex) {
            // Mempool empty or chain not initialized
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", ex.getMessage()));
        }
    }
}