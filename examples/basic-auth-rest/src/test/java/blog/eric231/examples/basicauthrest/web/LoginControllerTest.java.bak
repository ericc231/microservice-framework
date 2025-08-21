package blog.eric231.examples.basicauthrest.web;

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

@WebMvcTest(LoginController.class)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationDomainLogic authDomainLogic;

    @Test
    void login_ShouldReturnLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void dashboard_WithAuth_ShouldReturnDashboard() throws Exception {
        Map<String, Object> mockUser = Map.of(
            "username", "testuser",
            "authenticated", true
        );
        
        when(authDomainLogic.getCurrentUser()).thenReturn(mockUser);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("service", "Basic Auth REST Service"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void profile_WithAuth_ShouldReturnProfile() throws Exception {
        Map<String, Object> mockUser = Map.of(
            "username", "testuser",
            "authenticated", true
        );
        
        when(authDomainLogic.getCurrentUser()).thenReturn(mockUser);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void admin_WithAdminRole_ShouldReturnAdminPage() throws Exception {
        Map<String, Object> mockUser = Map.of(
            "username", "admin",
            "authenticated", true,
            "isAdmin", true
        );
        
        when(authDomainLogic.getCurrentUser()).thenReturn(mockUser);
        when(authDomainLogic.hasRole("ADMIN")).thenReturn(true);

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("adminFeatures"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void admin_WithUserRole_ShouldReturnDashboardWithError() throws Exception {
        Map<String, Object> mockUser = Map.of(
            "username", "user",
            "authenticated", true,
            "isAdmin", false
        );
        
        when(authDomainLogic.getCurrentUser()).thenReturn(mockUser);
        when(authDomainLogic.hasRole("ADMIN")).thenReturn(false);

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }
}