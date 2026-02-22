package t_12.backend.api.coin;

import t_12.backend.service.CoinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coin")
public class CoinController {

    private final CoinService coinService;

    public CoinController(CoinService coinService) {
        this.coinService = coinService;
    }

    @GetMapping
    public ResponseEntity<CoinDTO> getCoin() {
        return ResponseEntity.ok(new CoinDTO(coinService.getCurrentCoin()));
    }
}