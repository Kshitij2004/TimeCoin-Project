package t_12.backend.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.service.UserService;

/**
 * Controller for handling authentication-related HTTP requests.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    // Constructor injection, same pattern as WalletController.
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Registers a new user with the provided details.
     *
     * @param request the registration request containing username, email, and
     * password
     * @return ResponseEntity containing the created UserDTO
     */
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody RegisterRequest request) {

        // Pass the raw fields to the service, which handles validation,
        // hashing, and wallet creation. We get back the saved User entity.
        // We then wrap it in a UserDTO before returning so the password is never exposed.
        UserDTO dto = new UserDTO(userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        ));
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
