package t_12.backend.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import t_12.backend.entity.MiningAccumulator;
import t_12.backend.repository.MiningAccumulatorRepository;

@ExtendWith(MockitoExtension.class)
public class MiningAggregationSchedulerTest {

    @Mock
    private MiningService miningService;

    @Mock
    private MiningAccumulatorRepository miningAccumulatorRepository;

    @InjectMocks
    private MiningAggregationScheduler miningAggregationScheduler;

    @Test
    void flush_whenEnabledAndRowsPresent_callsFlushAccumulator() {
        ReflectionTestUtils.setField(miningAggregationScheduler, "miningEnabled", true);
        List<MiningAccumulator> rows = List.of(new MiningAccumulator("addr_1"));
        when(miningAccumulatorRepository.findAll())
                .thenReturn(rows);

        miningAggregationScheduler.flush();

        verify(miningService).flushAccumulator(rows);
    }

    @Test
    void flush_whenDisabled_doesNotFlush() {
        ReflectionTestUtils.setField(miningAggregationScheduler, "miningEnabled", false);

        miningAggregationScheduler.flush();

        verify(miningService, never()).flushAccumulator(any());
        verify(miningAccumulatorRepository, never())
                .findAll();
    }

    @Test
    void flush_whenEnabledButNoRows_doesNotFlush() {
        ReflectionTestUtils.setField(miningAggregationScheduler, "miningEnabled", true);
        when(miningAccumulatorRepository.findAll())
                .thenReturn(List.of());

        miningAggregationScheduler.flush();

        verify(miningService, never()).flushAccumulator(any());
    }
}
