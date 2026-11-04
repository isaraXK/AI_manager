package com.aimanager.profile;

import com.aimanager.auth.OwnerAuthService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.aimanager.auth.OwnerAuthService.SESSION_COOKIE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OwnerProfileControllerTest {
    private static final String PATH = "/api/v1/profile";
    private static final Cookie SESSION = new Cookie(SESSION_COOKIE, "test-session");
    private static final String FIRST_PROFILE = """
            {"version":0,"profile":{"nickname":" 小林 ","gender":"女","birthDate":"1998-05-20"}}
            """;

    private final OwnerAuthService auth = mock(OwnerAuthService.class);
    private final OwnerProfileService profiles = mock(OwnerProfileService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new OwnerProfileController(auth, profiles)).build();

    @Test
    void profileRequiresAnAuthenticatedSession() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(put(PATH).header("X-AI-Manager-Request", "1")
                        .contentType("application/json").content(FIRST_PROFILE))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(profiles);
    }

    @Test
    void firstVisitReturnsVersionZero() throws Exception {
        authenticate();
        when(profiles.find(1L)).thenReturn(new OwnerProfileService.ProfileResponse(false, 0, null, null, null));
        mvc.perform(get(PATH).cookie(SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileReady").value(false))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void saveWithoutRequestMarkerCannotWriteProfile() throws Exception {
        authenticate();
        mvc.perform(put(PATH).cookie(SESSION).contentType("application/json").content(FIRST_PROFILE))
                .andExpect(status().isForbidden());
        verifyNoInteractions(profiles);
    }

    @Test
    void blankNicknameAndFutureBirthDateAreRejected() throws Exception {
        authenticate();
        mvc.perform(put(PATH).cookie(SESSION).header("X-AI-Manager-Request", "1")
                        .contentType("application/json").content("""
                                {"version":0,"profile":{"nickname":" ","gender":"女","birthDate":"9999-12-31"}}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.nickname").exists())
                .andExpect(jsonPath("$.details.birthDate").exists());
        verifyNoInteractions(profiles);
    }

    @Test
    void firstSaveReturnsReadyStatusAndNewVersion() throws Exception {
        authenticate();
        var fields = new ProfileFields("小林", "女", LocalDate.of(1998, 5, 20),
                null, null, null, null, null, null, null);
        when(profiles.save(1L, 0, fields)).thenReturn(new OwnerProfileService.ProfileResponse(
                true, 1, fields, 28, Instant.parse("2026-11-04T00:00:00Z")));
        mvc.perform(put(PATH).cookie(SESSION).header("X-AI-Manager-Request", "1")
                        .contentType("application/json").content(FIRST_PROFILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileReady").value(true))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.profile.nickname").value("小林"));
    }

    @Test
    void staleVersionIsReportedAsAConflict() throws Exception {
        authenticate();
        when(profiles.save(eq(1L), eq(1L), any(ProfileFields.class)))
                .thenThrow(new OwnerProfileService.VersionConflict());
        mvc.perform(put(PATH).cookie(SESSION).header("X-AI-Manager-Request", "1")
                        .contentType("application/json").content(FIRST_PROFILE.replace("\"version\":0", "\"version\":1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    private void authenticate() {
        when(auth.findSession("test-session")).thenReturn(Optional.of(new OwnerAuthService.Session(1L, false)));
    }
}
