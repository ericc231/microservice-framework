package blog.eric231.examples.basicauthrest.api;

import blog.eric231.examples.basicauthrest.domain.AuthenticationDomainLogic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationDomainLogic authDomainLogic;

    @Test
    void getAuthStatus_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getAuthStatus_WithAuth_ShouldReturnStatus() throws Exception {
        Map<String, Object> mockStatus = Map.of(
            "authenticated", true,
            "authType", "Basic Authentication"
        );
        
        when(authDomainLogic.getAuthenticationStatus()).thenReturn(mockStatus);

        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.authenticated").value(true))
                .andExpected(jsonPath("$.authType").value("Basic Authentication"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void processAuth_WithMessage_ShouldReturnProcessedResult() throws Exception {
        Map<String, Object> mockResult = Map.of(
            "message", "Test message",
            "user", Map.of("username", "testuser")
        );
        
        when(authDomainLogic.processAuthenticationRequest("Test message")).thenReturn(mockResult);

        mockMvc.perform(post("/api/auth/process")
                .param("message", "Test message"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.message").value("Test message"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void checkRole_WithAdminRole_ShouldReturnTrue() throws Exception {
        when(authDomainLogic.hasRole("ADMIN")).thenReturn(true);
        when(authDomainLogic.getCurrentUser()).thenReturn(Map.of("username", "admin"));

        mockMvc.perform(get("/api/auth/role/ADMIN"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.hasRole").value(true))
                .andExpected(jsonPath("$.role").value("ADMIN"));
    }
}