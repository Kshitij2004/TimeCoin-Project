package t_12.backend.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.service.UserRegistrationResult;
import t_12.backend.service.UserService;

/**
 * Controller for handling authentication-related HTTP requests.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final boolean exposePrivateKeyOnCreate;

    /**
     * Creates an auth controller with user service and registration response mode.
     *
     * @param userService user business service
     * @param exposePrivateKeyOnCreate true when register response may include private key
     */
    public AuthController(
            UserService userService,
            @Value("${wallet.expose-private-key-on-create:true}") boolean exposePrivateKeyOnCreate) {
        this.userService = userService;
        this.exposePrivateKeyOnCreate = exposePrivateKeyOnCreate;
    }

    /**
     * Registers a new user with the provided details.
     *
     * @param request the registration request containing username, email, and
     * password
     * @return ResponseEntity containing created user and wallet metadata
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody RegisterRequest request) {
        UserRegistrationResult registrationResult = userService.registerWithWallet(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );

        RegisterResponseDTO dto = new RegisterResponseDTO(
                registrationResult,
                exposePrivateKeyOnCreate
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Authenticates a user and returns a JWT token if successful.
     *
     * @param request the login request containing username and password
     * @return ResponseEntity containing the JWT token string
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(token);
    }

}
