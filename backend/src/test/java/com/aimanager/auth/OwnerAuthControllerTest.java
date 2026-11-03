package com.aimanager.auth;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OwnerAuthControllerTest {
    private final OwnerAuthService auth = mock(OwnerAuthService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new OwnerAuthController(auth, false)).build();

    @Test
    void loginWithoutRequestMarkerCannotCreateSession() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("""
                                {"username":"owner","password":"example-password"}
                                """))
                .andExpect(status().isForbidden());
        verifyNoInteractions(auth);
    }

    @Test
    void loginReturnsHttpOnlyCookieAndProfileStatus() throws Exception {
        when(auth.login("owner", "example-password"))
                .thenReturn(Optional.of(new OwnerAuthService.Login("example-session-token", false)));
        mvc.perform(post("/api/v1/auth/login").header("X-AI-Manager-Request", "1")
                        .contentType("application/json")
                        .content("""
                                {"username":"owner","password":"example-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=604800")))
                .andExpect(jsonPath("$.loggedIn").value(true))
                .andExpect(jsonPath("$.profileReady").value(false))
                .andExpect(jsonPath("$.token").doesNotExist());
    }
}
