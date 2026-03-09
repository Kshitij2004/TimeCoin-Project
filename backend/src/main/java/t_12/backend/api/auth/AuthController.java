package t_12.backend.api.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import t_12.backend.service.UserService;

// @RestController tells Spring this class handles HTTP requests
// and that return values should be written directly to the response body as JSON.
// @RequestMapping sets the base URL for all endpoints in this controller.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    // Constructor injection, same pattern as WalletController.
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // @PostMapping means this method handles POST requests to /api/auth/register.
    // @RequestBody tells Spring to parse the incoming JSON body into a RegisterRequest object.
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody RegisterRequest request) {

        // Pass the raw fields to the service, which handles validation,
        // hashing, and wallet creation. We get back the saved User entity.
        // We then wrap it in a UserDTO before returning so the password is never exposed.
        return ResponseEntity.ok(new UserDTO(userService.register(
            request.getUsername(),
            request.getEmail(),
            request.getPassword()
        )));
    }
}