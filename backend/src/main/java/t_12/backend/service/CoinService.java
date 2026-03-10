package t_12.backend.service;

import org.springframework.stereotype.Service;

import t_12.backend.entity.Coin;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.CoinRepository;

/**
 * Service class for handling coin-related business logic. Provides methods to
 * retrieve coin data from the repository.
 */
@Service
public class CoinService {

    private final CoinRepository coinRepository;

    public CoinService(CoinRepository coinRepository) {
        this.coinRepository = coinRepository;
    }

    /**
     * Retrieves the current coin information. Assumes there is only one coin
     * entity in the database.
     *
     * @return the Coin entity representing current coin data
     * @throws ResourceNotFoundException if no coin data is found
     */
    public Coin getCurrentCoin() {
        return coinRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                "No coin data found"
        ));
    }
}
