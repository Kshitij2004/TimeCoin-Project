package t_12.backend.api.blockchain;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.api.blockchain.dto.BlockDetailDTO;
import t_12.backend.api.blockchain.dto.BlockListResponseDTO;
import t_12.backend.api.blockchain.dto.ChainStatusDTO;
import t_12.backend.service.BlockchainExplorerService;

/**
 * Explorer endpoints for block inspection and chain status queries.
 */
@RestController
@RequestMapping("/api/chain")
public class BlockchainExplorerController {

    private final BlockchainExplorerService blockchainExplorerService;

    public BlockchainExplorerController(BlockchainExplorerService blockchainExplorerService) {
        this.blockchainExplorerService = blockchainExplorerService;
    }

    @GetMapping("/status")
    public ResponseEntity<ChainStatusDTO> getStatus() {
        return ResponseEntity.ok(blockchainExplorerService.getChainStatus());
    }

    @GetMapping("/blocks")
    public ResponseEntity<BlockListResponseDTO> getBlocks(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(blockchainExplorerService.getRecentBlocks(page, limit));
    }

    @GetMapping("/blocks/{height}")
    public ResponseEntity<BlockDetailDTO> getBlockByHeight(@PathVariable Integer height) {
        return ResponseEntity.ok(blockchainExplorerService.getBlockByHeight(height));
    }

    @GetMapping("/blocks/hash/{hash}")
    public ResponseEntity<BlockDetailDTO> getBlockByHash(@PathVariable String hash) {
        return ResponseEntity.ok(blockchainExplorerService.getBlockByHash(hash));
    }
}
