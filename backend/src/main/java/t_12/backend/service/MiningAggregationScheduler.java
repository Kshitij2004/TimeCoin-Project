package t_12.backend.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import t_12.backend.entity.MiningAccumulator;
import t_12.backend.repository.MiningAccumulatorRepository;

/**
 * Scheduled job that flushes accumulated mining clicks into coinbase
 * transactions at a fixed interval. Mirrors the pattern used by
 * BlockAssemblyScheduler. Fires on a fixed delay configured via
 * application.properties.
 */
@Component
public class MiningAggregationScheduler {

    private final MiningService miningService;
    private final MiningAccumulatorRepository miningAccumulatorRepository;

    @Value("${mining.enabled}")
    private boolean miningEnabled;

    public MiningAggregationScheduler(
            MiningService miningService,
            MiningAccumulatorRepository miningAccumulatorRepository) {
        this.miningService = miningService;
        this.miningAccumulatorRepository = miningAccumulatorRepository;
    }

    /**
     * Fetches all active accumulator rows and delegates to MiningService to
     * flush them into coinbase transactions. Skips silently if mining is
     * disabled or no rows are present.
     */
    @Scheduled(fixedDelayString = "${mining.window-seconds}", timeUnit = TimeUnit.SECONDS)
    public void flush() {
        if (!miningEnabled) {
            return;
        }

        List<MiningAccumulator> rows = miningAccumulatorRepository.findAll();

        if (rows.isEmpty()) {
            return;
        }

        miningService.flushAccumulator(rows);
    }
}
