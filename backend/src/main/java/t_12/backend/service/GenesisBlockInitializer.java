package t_12.backend.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ensures blockchain genesis is present when the application starts.
 */
@Component
public class GenesisBlockInitializer {

    private final BlockService blockService;

    public GenesisBlockInitializer(BlockService blockService) {
        this.blockService = blockService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureGenesisBlock() {
        blockService.createGenesisBlock();
    }
}
