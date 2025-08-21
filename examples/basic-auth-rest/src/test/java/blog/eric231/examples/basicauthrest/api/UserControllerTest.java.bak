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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationDomainLogic authDomainLogic;

    @Test
    void getCurrentUser_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getCurrentUser_WithAuth_ShouldReturnUserInfo() throws Exception {
        Map<String, Object> mockUser = Map.of(
            "username", "testuser",
            "authenticated", true,
            "authorities", java.util.Set.of("ROLE_USER")
        );
        
        when(authDomainLogic.getCurrentUser()).thenReturn(mockUser);

        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAdminInfo_WithAdminRole_ShouldReturnAdminInfo() throws Exception {
        Map<String, Object> mockUser = Map.of(
            "username", "admin",
            "authenticated", true,
            "authorities", java.util.Set.of("ROLE_ADMIN")
        );
        
        when(authDomainLogic.getCurrentUser()).thenReturn(mockUser);

        mockMvc.perform(get("/api/user/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin access granted"))
                .andExpect(jsonPath("$.adminFeatures").exists());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getAdminInfo_WithUserRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/user/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void echo_WithAuth_ShouldReturnEcho() throws Exception {
        Map<String, Object> mockResult = Map.of(
            "message", "test message",
            "user", Map.of("username", "testuser")
        );
        
        when(authDomainLogic.processAuthenticationRequest("test message")).thenReturn(mockResult);

        mockMvc.perform(get("/api/user/echo").param("message", "test message"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("test message"));
    }
}