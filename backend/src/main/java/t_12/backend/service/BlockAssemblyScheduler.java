package t_12.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers block assembly on a configurable fixed-rate interval.
 *
 * Delegates to BlockAssemblerService. If the mempool is empty, the attempt is
 * silently skipped - no block is created and no spam is logged. Assembly can be
 * disabled entirely via application.properties.
 */
@Component
public class BlockAssemblyScheduler {

    private static final Logger log = LoggerFactory.getLogger(BlockAssemblyScheduler.class);

    private final BlockAssemblerService blockAssemblerService;

    // Configurable via application.properties. Defaults to true (enabled).
    @Value("${block.assembly.enabled:true}")
    private boolean enabled;

    public BlockAssemblyScheduler(BlockAssemblerService blockAssemblerService) {
        this.blockAssemblerService = blockAssemblerService;
    }

    /**
     * Runs on a fixed delay defined by block.assembly.interval (in
     * milliseconds). Skips silently if assembly is disabled or the mempool is
     * empty.
     *
     * By default, runs every 30 seconds. Adjust as needed for testing or
     * production.
     */
    @Scheduled(fixedDelayString = "${block.assembly.interval:30000}")
    public void scheduledAssembly() {
        log.info("Scheduled block assembly fired. enabled={}", enabled);
        if (!enabled) {
            return;
        }

        try {
            blockAssemblerService.assembleAndCommit(null);
        } catch (IllegalStateException ex) {
            // Empty mempool - expected, not an error
            log.debug("Scheduled assembly skipped: {}", ex.getMessage());
        }
    }
}
