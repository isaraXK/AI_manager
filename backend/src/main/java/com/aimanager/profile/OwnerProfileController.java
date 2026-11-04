package com.aimanager.profile;

import com.aimanager.auth.OwnerAuthService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.aimanager.auth.OwnerAuthService.SESSION_COOKIE;

@RestController
@Profile("!smoke")
@RequestMapping("/api/v1/profile")
class OwnerProfileController {
    private final OwnerAuthService auth;
    private final OwnerProfileService profiles;

    OwnerProfileController(OwnerAuthService auth, OwnerProfileService profiles) {
        this.auth = auth;
        this.profiles = profiles;
    }

    @GetMapping
    ResponseEntity<?> get(@CookieValue(value = SESSION_COOKIE, required = false) String token) {
        var session = auth.findSession(token);
        if (session.isEmpty()) {
            return unauthorized();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(profiles.find(session.get().ownerId()));
    }

    @PutMapping
    ResponseEntity<?> put(
            @CookieValue(value = SESSION_COOKIE, required = false) String token,
            @RequestHeader(value = "X-AI-Manager-Request", required = false) String requestMarker,
            @RequestBody SaveProfileRequest request) {
        var session = auth.findSession(token);
        if (session.isEmpty()) {
            return unauthorized();
        }
        if (!"1".equals(requestMarker)) {
            return error(403, "REQUEST_MARKER_REQUIRED", "请求缺少同源校验标记", Map.of());
        }
        ProfileFields fields = request.profile() == null ? null : request.profile().normalized();
        Map<String, String> errors = validate(request.version(), fields);
        if (!errors.isEmpty()) {
            return error(422, "PROFILE_VALIDATION_FAILED", "请检查个人资料", errors);
        }
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(profiles.save(session.get().ownerId(), request.version(), fields));
        } catch (OwnerProfileService.VersionConflict conflict) {
            return error(409, "VERSION_CONFLICT", "资料已更新，请重新读取后再保存", Map.of());
        }
    }

    private static Map<String, String> validate(Long version, ProfileFields fields) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (version == null || version < 0) {
            errors.put("version", "请提供有效的资料版本");
        }
        if (fields == null) {
            errors.put("profile", "请填写个人资料");
            return errors;
        }
        if (fields.nickname() == null) {
            errors.put("nickname", "请填写称呼");
        }
        if (fields.gender() == null) {
            errors.put("gender", "请填写性别");
        }
        if (fields.birthDate() == null) {
            errors.put("birthDate", "请填写生日");
        } else if (fields.birthDate().getYear() < 1000
                || fields.birthDate().isAfter(LocalDate.now(OwnerProfileService.BUSINESS_ZONE))) {
            errors.put("birthDate", "请选择有效且不晚于今天的生日");
        }
        checkLength(errors, "nickname", fields.nickname(), 100);
        checkLength(errors, "gender", fields.gender(), 32);
        checkLength(errors, "occupation", fields.occupation(), 200);
        checkLength(errors, "mbti", fields.mbti(), 16);
        checkLength(errors, "zodiac", fields.zodiac(), 32);
        checkLength(errors, "bloodType", fields.bloodType(), 16);
        checkText(errors, "routine", fields.routine());
        checkText(errors, "interests", fields.interests());
        checkText(errors, "selfIntro", fields.selfIntro());
        return errors;
    }

    private static void checkLength(Map<String, String> errors, String field, String value, int maxLength) {
        if (value != null && value.codePointCount(0, value.length()) > maxLength) {
            errors.put(field, "最多填写 " + maxLength + " 个字符");
        }
    }

    private static void checkText(Map<String, String> errors, String field, String value) {
        if (value != null && value.getBytes(StandardCharsets.UTF_8).length > 65_535) {
            errors.put(field, "内容过长，请缩短后再保存");
        }
    }

    private static ResponseEntity<?> unauthorized() {
        return error(401, "UNAUTHORIZED", "请先登录", Map.of());
    }

    private static ResponseEntity<?> error(int status, String code, String message, Map<String, String> details) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore())
                .body(Map.of("code", code, "message", message, "details", details));
    }

    record SaveProfileRequest(Long version, ProfileFields profile) {}
}
