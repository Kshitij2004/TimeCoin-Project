package t_12.backend.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.entity.Coin;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.CoinRepository;

@ExtendWith(MockitoExtension.class)
class CoinServiceTest {

    @Mock
    private CoinRepository coinRepository;

    @InjectMocks
    private CoinService coinService;

    @Test
    void GetCurrentCoin_ReturnsCoin_WhenFoundTest() {
        Coin coin = new Coin();
        coin.setCurrentPrice(new BigDecimal("10.00"));
        coin.setTotalSupply(new BigDecimal("1000000.00"));
        coin.setCirculatingSupply(new BigDecimal("500000.00"));

        when(coinRepository.findAll()).thenReturn(List.of(coin));

        Coin result = coinService.getCurrentCoin();

        assertEquals(new BigDecimal("10.00"), result.getCurrentPrice());
        assertEquals(new BigDecimal("1000000.00"), result.getTotalSupply());
        verify(coinRepository, times(1)).findAll();
    }

    @Test
    void GetCurrentCoin_ThrowsException_WhenNoneFoundTest() {
        when(coinRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> {
            coinService.getCurrentCoin();
        });

        verify(coinRepository, times(1)).findAll();
    }
}