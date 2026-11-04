package com.aimanager.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!smoke")
public class OwnerAuthService {
    public static final String SESSION_COOKIE = "ai_manager_session";
    static final Duration SESSION_DURATION = Duration.ofDays(7);
    private static final long OWNER_ID = 1L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final PasswordHasher passwords = new PasswordHasher();

    OwnerAuthService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void initialize(String username, String password) {
        if (username.isBlank() || username.length() > 100 || password.length() < 6
                || password.length() > 256) {
            throw new IllegalArgumentException("Owner setup requires a username and a 6–256 character password");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM owner_account WHERE id = ?", Integer.class, OWNER_ID) > 0) {
            throw new IllegalStateException("Owner already initialized. Remove the setup environment variables.");
        }
        jdbc.update("INSERT INTO owner_account (id, username, password_hash) VALUES (?, ?, ?)",
                OWNER_ID, username, passwords.hash(password));
    }

    @Transactional
    public Optional<Login> login(String username, String password) {
        if (username == null || username.isBlank() || username.length() > 100
                || password == null || password.isEmpty() || password.length() > 256) {
            return Optional.empty();
        }
        var accounts = jdbc.query("SELECT id, username, password_hash FROM owner_account WHERE id = ?",
                (rs, row) -> new Account(rs.getLong("id"), rs.getString("username"),
                        rs.getString("password_hash")), OWNER_ID);
        if (accounts.isEmpty()) {
            return Optional.empty();
        }
        Account account = accounts.get(0);
        if (!account.username().equals(username) || !passwords.matches(password, account.passwordHash())) {
            return Optional.empty();
        }
        LocalDateTime now = now();
        jdbc.update("DELETE FROM owner_session WHERE owner_id = ? AND expires_at <= ?", account.id(), now);
        byte[] token = new byte[32];
        RANDOM.nextBytes(token);
        jdbc.update("INSERT INTO owner_session (token_hash, owner_id, expires_at) VALUES (?, ?, ?)",
                digest(token), account.id(), now.plus(SESSION_DURATION));
        return Optional.of(new Login(Base64.getUrlEncoder().withoutPadding().encodeToString(token),
                profileReady(account.id())));
    }

    public Optional<Session> findSession(String token) {
        return tokenHash(token).flatMap(hash -> jdbc.query("""
                SELECT s.owner_id, p.owner_id IS NOT NULL AS profile_ready
                FROM owner_session s LEFT JOIN owner_profile p ON p.owner_id = s.owner_id
                WHERE s.token_hash = ? AND s.expires_at > ?
                """, (rs, row) -> new Session(rs.getLong("owner_id"), rs.getBoolean("profile_ready")),
                hash, now()).stream().findFirst());
    }

    void logout(String token) {
        tokenHash(token).ifPresent(hash -> jdbc.update("DELETE FROM owner_session WHERE token_hash = ?", hash));
    }

    private boolean profileReady(long ownerId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM owner_profile WHERE owner_id = ?", Integer.class, ownerId) > 0;
    }

    private Optional<byte[]> tokenHash(String token) {
        if (token == null || token.length() != 43) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(token);
            return bytes.length == 32 ? Optional.of(digest(bytes)) : Optional.empty();
        } catch (IllegalArgumentException invalidToken) {
            return Optional.empty();
        }
    }

    private static byte[] digest(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Session hashing unavailable", exception);
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    record Login(String token, boolean profileReady) {}
    public record Session(long ownerId, boolean profileReady) {}
    private record Account(long id, String username, String passwordHash) {}
}
