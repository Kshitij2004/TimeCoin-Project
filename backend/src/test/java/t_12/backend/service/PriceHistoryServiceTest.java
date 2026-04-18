package t_12.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.entity.PriceHistory;
import t_12.backend.repository.PriceHistoryRepository;

@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceTest {

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private PriceHistoryService priceHistoryService;

    @Test
    void getAll_returnsChronologicalOrder() {
        LocalDateTime t1 = LocalDateTime.of(2026, 4, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 4, 2, 10, 0);
        PriceHistory p1 = new PriceHistory(new BigDecimal("10.00"), t1);
        PriceHistory p2 = new PriceHistory(new BigDecimal("10.50"), t2);

        when(priceHistoryRepository.findAllByOrderByRecordedAtAsc())
                .thenReturn(List.of(p1, p2));

        List<PriceHistory> result = priceHistoryService.getAll();

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("10.00"), result.get(0).getPrice());
        assertEquals(new BigDecimal("10.50"), result.get(1).getPrice());
    }

    @Test
    void getAll_returnsEmptyList_whenNoHistory() {
        when(priceHistoryRepository.findAllByOrderByRecordedAtAsc())
                .thenReturn(List.of());

        List<PriceHistory> result = priceHistoryService.getAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void getSince_filtersOlderEntries() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 4, 10, 0, 0);
        LocalDateTime t1 = LocalDateTime.of(2026, 4, 11, 10, 0);
        PriceHistory p1 = new PriceHistory(new BigDecimal("12.00"), t1);

        when(priceHistoryRepository.findByRecordedAtAfterOrderByRecordedAtAsc(cutoff))
                .thenReturn(List.of(p1));

        List<PriceHistory> result = priceHistoryService.getSince(cutoff);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("12.00"), result.get(0).getPrice());
    }

    @Test
    void getSince_returnsEmpty_whenNothingAfterCutoff() {
        LocalDateTime cutoff = LocalDateTime.now();

        when(priceHistoryRepository.findByRecordedAtAfterOrderByRecordedAtAsc(cutoff))
                .thenReturn(List.of());

        List<PriceHistory> result = priceHistoryService.getSince(cutoff);

        assertTrue(result.isEmpty());
    }
}