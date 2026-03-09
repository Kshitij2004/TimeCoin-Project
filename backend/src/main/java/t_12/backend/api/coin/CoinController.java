package t_12.backend.api.coin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.service.CoinService;

/**
 * Controller for handling coin-related HTTP requests.
 */
@RestController
@RequestMapping("/api/coin")
public class CoinController {

    private final CoinService coinService;

    public CoinController(CoinService coinService) {
        this.coinService = coinService;
    }

    /**
     * Retrieves the current coin information.
     *
     * @return ResponseEntity containing the CoinDTO with current coin data
     */
    @GetMapping
    public ResponseEntity<CoinDTO> getCoin() {
        return ResponseEntity.ok(new CoinDTO(coinService.getCurrentCoin()));
    }
}
