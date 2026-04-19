package t_12.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import t_12.backend.entity.PriceHistory;
import t_12.backend.repository.PriceHistoryRepository;

/**
 * Service for retrieving historical price data.
 * Supports optional time-range filtering for frontend charting.
 */
@Service
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;

    public PriceHistoryService(PriceHistoryRepository priceHistoryRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
    }

    /**
     * Returns all price history entries in chronological order.
     */
    public List<PriceHistory> getAll() {
        return priceHistoryRepository.findAllByOrderByRecordedAtAsc();
    }

    /**
     * Returns price history entries recorded after the given timestamp.
     *
     * @param since only return entries after this time
     */
    public List<PriceHistory> getSince(LocalDateTime since) {
        return priceHistoryRepository.findByRecordedAtAfterOrderByRecordedAtAsc(since);
    }
}