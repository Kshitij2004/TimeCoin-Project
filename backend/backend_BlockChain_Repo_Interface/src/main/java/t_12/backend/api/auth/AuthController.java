package t_12.backend.api.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.service.UserService;

/**
 * Exposes authentication-related HTTP endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Delegates registration work to the service layer.
    private final UserService userService;

    /**
     * Injects the user service used by this controller.
     */
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Registers a new user account and returns a safe response DTO.
     */
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(new UserDTO(userService.register(
            request.getUsername(),
            request.getEmail(),
            request.getPassword()
        )));
    }
}
