package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.entity.Coin;
import t_12.backend.entity.PriceHistory;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.CoinRepository;
import t_12.backend.repository.PriceHistoryRepository;

@ExtendWith(MockitoExtension.class)
class PriceEngineServiceTest {

    @Mock
    private CoinRepository coinRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private PriceEngineService priceEngineService;

    private Coin coin;

    @BeforeEach
    void setUp() {
        coin = new Coin();
        coin.setId(1L);
        coin.setCurrentPrice(new BigDecimal("100.00"));
        coin.setCirculatingSupply(new BigDecimal("1000.00"));
        coin.setTotalSupply(new BigDecimal("1000000.00"));
    }

    @Test
    void recordBuy_increasesPrice() {
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));

        priceEngineService.recordBuy(new BigDecimal("10.00"));

        assertEquals(new BigDecimal("100.01"), coin.getCurrentPrice());
        verify(coinRepository).save(coin);
    }

    @Test
    void recordBuy_largeTrade_biggerImpact() {
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));

        priceEngineService.recordBuy(new BigDecimal("500.00"));

        assertEquals(new BigDecimal("100.50"), coin.getCurrentPrice());
    }

    @Test
    void recordSell_decreasesPrice() {
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));

        priceEngineService.recordSell(new BigDecimal("10.00"));

        assertEquals(new BigDecimal("99.99"), coin.getCurrentPrice());
        verify(coinRepository).save(coin);
    }

    @Test
    void recordSell_largeTrade_biggerImpact() {
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));

        priceEngineService.recordSell(new BigDecimal("500.00"));

        assertEquals(new BigDecimal("99.50"), coin.getCurrentPrice());
    }

    @Test
    void recordSell_priceDoesNotDropBelowFloor() {
        coin.setCurrentPrice(new BigDecimal("0.02"));
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));

        priceEngineService.recordSell(new BigDecimal("900.00"));

        assertTrue(coin.getCurrentPrice().compareTo(new BigDecimal("0.01")) >= 0);
    }

    @Test
    void recordBuy_priceDoesNotExceedCeiling() {
        coin.setCurrentPrice(new BigDecimal("999999.99"));
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));

        priceEngineService.recordBuy(new BigDecimal("900.00"));

        assertTrue(coin.getCurrentPrice().compareTo(new BigDecimal("1000000.00")) <= 0);
    }

    @Test
    void recordBuy_savesPriceHistorySnapshot() {
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));

        priceEngineService.recordBuy(new BigDecimal("10.00"));

        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        assertEquals(new BigDecimal("100.01"), captor.getValue().getPrice());
    }

    @Test
    void recordSell_savesPriceHistorySnapshot() {
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));

        priceEngineService.recordSell(new BigDecimal("10.00"));

        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        assertEquals(new BigDecimal("99.99"), captor.getValue().getPrice());
    }

    @Test
    void recordBuy_zeroSupply_noChange() {
        coin.setCirculatingSupply(BigDecimal.ZERO);
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));

        priceEngineService.recordBuy(new BigDecimal("10.00"));

        assertEquals(new BigDecimal("100.00"), coin.getCurrentPrice());
        verify(coinRepository, never()).save(any());
    }

    @Test
    void recordBuy_noCoinFound_throwsException() {
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> priceEngineService.recordBuy(new BigDecimal("10.00")));
    }
}