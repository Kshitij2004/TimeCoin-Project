package t_12.backend.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenesisBlockInitializerTest {

    @Mock
    private BlockService blockService;

    @InjectMocks
    private GenesisBlockInitializer genesisBlockInitializer;

    @Test
    void ensureGenesisBlock_callsCreateGenesisBlock() {
        genesisBlockInitializer.ensureGenesisBlock();

        verify(blockService).createGenesisBlock();
    }
}
