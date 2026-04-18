package t_12.backend.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class BlockAssemblySchedulerTest {

    @Mock
    private BlockAssemblerService blockAssemblerService;

    @InjectMocks
    private BlockAssemblyScheduler blockAssemblyScheduler;

    @Test
    void scheduledAssembly_whenEnabled_callsAssembleAndCommit() {
        ReflectionTestUtils.setField(blockAssemblyScheduler, "enabled", true);

        blockAssemblyScheduler.scheduledAssembly();

        verify(blockAssemblerService, times(1)).assembleAndCommit(null);
    }

    @Test
    void scheduledAssembly_whenDisabled_doesNotCallAssembleAndCommit() {
        ReflectionTestUtils.setField(blockAssemblyScheduler, "enabled", false);

        blockAssemblyScheduler.scheduledAssembly();

        verify(blockAssemblerService, never()).assembleAndCommit(null);
    }

    @Test
    void scheduledAssembly_whenMempoolEmpty_doesNotThrow() {
        ReflectionTestUtils.setField(blockAssemblyScheduler, "enabled", true);
        doThrow(new IllegalStateException("No pending transactions in mempool. Block assembly aborted."))
                .when(blockAssemblerService).assembleAndCommit(null);

        // Should complete without throwing
        blockAssemblyScheduler.scheduledAssembly();

        verify(blockAssemblerService, times(1)).assembleAndCommit(null);
    }
}
