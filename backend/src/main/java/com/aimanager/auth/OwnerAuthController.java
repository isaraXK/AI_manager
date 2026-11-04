package com.aimanager.auth;

import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.aimanager.auth.OwnerAuthService.SESSION_COOKIE;

@RestController
@Profile("!smoke")
@RequestMapping("/api/v1/auth")
class OwnerAuthController {
    private final OwnerAuthService auth;
    private final boolean secureCookie;

    OwnerAuthController(OwnerAuthService auth, @Value("${ai-manager.auth.secure-cookie:false}") boolean secureCookie) {
        this.auth = auth;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@RequestHeader(value = "X-AI-Manager-Request", required = false) String requestMarker,
            @RequestBody LoginRequest request) {
        if (!"1".equals(requestMarker)) {
            return error(403, "REQUEST_MARKER_REQUIRED", "请求缺少同源校验标记");
        }
        var login = auth.login(request.username(), request.password());
        if (login.isEmpty()) {
            return error(401, "INVALID_CREDENTIALS", "用户名或密码不正确");
        }
        var result = login.get();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(result.token(), OwnerAuthService.SESSION_DURATION))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new SessionStatus(true, result.profileReady()));
    }

    @GetMapping("/session")
    ResponseEntity<SessionStatus> session(@CookieValue(value = SESSION_COOKIE, required = false) String token) {
        SessionStatus status = auth.findSession(token)
                .map(session -> new SessionStatus(true, session.profileReady()))
                .orElse(new SessionStatus(false, false));
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(status);
    }

    @PostMapping("/logout")
    ResponseEntity<?> logout(@RequestHeader(value = "X-AI-Manager-Request", required = false) String requestMarker,
            @CookieValue(value = SESSION_COOKIE, required = false) String token) {
        if (!"1".equals(requestMarker)) {
            return error(403, "REQUEST_MARKER_REQUIRED", "请求缺少同源校验标记");
        }
        auth.logout(token);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build();
    }

    private String cookie(String token, Duration age) {
        return ResponseCookie.from(SESSION_COOKIE, token).httpOnly(true).secure(secureCookie)
                .sameSite("Lax").path("/").maxAge(age).build().toString();
    }

    private ResponseEntity<?> error(int status, String code, String message) {
        return ResponseEntity.status(status).header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("code", code, "message", message, "details", Map.of()));
    }

    record LoginRequest(String username, String password) {}
    record SessionStatus(boolean loggedIn, boolean profileReady) {}
}
