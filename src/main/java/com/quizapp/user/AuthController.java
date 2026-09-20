package com.quizapp.user;

import com.quizapp.common.util.AuthContext;
import com.quizapp.user.dto.AuthResponse;
import com.quizapp.user.dto.LoginRequest;
import com.quizapp.user.dto.ProfileResponse;
import com.quizapp.user.dto.SignupRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public ProfileResponse me() {
        return authService.profile(AuthContext.currentUserId());
    }
}
