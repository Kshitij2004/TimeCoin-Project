package t_12.backend.service;

import t_12.backend.entity.Coin;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.CoinRepository;
import org.springframework.stereotype.Service;

@Service
public class CoinService {

    private final CoinRepository coinRepository;

    public CoinService(CoinRepository coinRepository) {
        this.coinRepository = coinRepository;
    }

    public Coin getCurrentCoin() {
        return coinRepository.findAll()
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                "No coin data found"
            ));
    }
}