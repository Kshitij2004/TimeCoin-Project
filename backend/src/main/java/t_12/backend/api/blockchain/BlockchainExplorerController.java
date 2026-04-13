package t_12.backend.api.blockchain;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.api.blockchain.dto.BlockDetailDTO;
import t_12.backend.api.blockchain.dto.BlockListResponseDTO;
import t_12.backend.api.blockchain.dto.ChainValidationReportDTO;
import t_12.backend.api.blockchain.dto.ChainStatusDTO;
import t_12.backend.service.BlockchainExplorerService;
import t_12.backend.service.ChainValidationService;

/**
 * Explorer endpoints for block inspection and chain status queries.
 */
@RestController
@RequestMapping("/api/chain")
public class BlockchainExplorerController {

    private final BlockchainExplorerService blockchainExplorerService;
    private final ChainValidationService chainValidationService;

    public BlockchainExplorerController(
            BlockchainExplorerService blockchainExplorerService,
            ChainValidationService chainValidationService) {
        this.blockchainExplorerService = blockchainExplorerService;
        this.chainValidationService = chainValidationService;
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

    @PostMapping("/mine-pending")
    public ResponseEntity<BlockDetailDTO> minePending(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "validatorAddress", required = false) String validatorAddress) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(blockchainExplorerService.minePendingTransactions(limit, validatorAddress));
    }

    @PostMapping("/validate")
    public ResponseEntity<ChainValidationReportDTO> validateChain() {
        return ResponseEntity.ok(chainValidationService.validateFullChain());
    }
}
