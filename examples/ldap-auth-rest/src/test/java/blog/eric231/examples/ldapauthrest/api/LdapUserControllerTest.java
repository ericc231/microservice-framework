package blog.eric231.examples.ldapauthrest.api;

import blog.eric231.examples.ldapauthrest.domain.LdapAuthenticationDomainLogic;
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

@WebMvcTest(LdapUserController.class)
class LdapUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LdapAuthenticationDomainLogic ldapDomainLogic;

    @Test
    void getCurrentUser_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "ben", roles = {"USER"})
    void getCurrentUser_WithAuth_ShouldReturnLdapUserInfo() throws Exception {
        Map<String, Object> mockUser = Map.of(
            "username", "ben",
            "authenticated", true,
            "authType", "LDAP",
            "dn", "uid=ben,ou=people,dc=springframework,dc=org",
            "authorities", java.util.Set.of("ROLE_USER")
        );
        
        when(ldapDomainLogic.getCurrentUser()).thenReturn(mockUser);

        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ben"))
                .andExpect(jsonPath("$.authType").value("LDAP"))
                .andExpect(jsonPath("$.dn").value("uid=ben,ou=people,dc=springframework,dc=org"));
    }

    @Test
    @WithMockUser(username = "bob", roles = {"ADMINS"})
    void getAdminInfo_WithAdminRole_ShouldReturnAdminInfo() throws Exception {
        Map<String, Object> mockUser = Map.of(
            "username", "bob",
            "authenticated", true,
            "authType", "LDAP",
            "dn", "uid=bob,ou=people,dc=springframework,dc=org",
            "authorities", java.util.Set.of("ROLE_ADMINS")
        );
        
        Map<String, Object> mockServerStatus = Map.of(
            "serverUrl", "ldap://localhost:8389",
            "connected", true
        );
        
        when(ldapDomainLogic.getCurrentUser()).thenReturn(mockUser);
        when(ldapDomainLogic.getLdapServerStatus()).thenReturn(mockServerStatus);

        mockMvc.perform(get("/api/user/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("LDAP Admin access granted"))
                .andExpect(jsonPath("$.adminFeatures").exists())
                .andExpect(jsonPath("$.ldapServer").exists());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getAdminInfo_WithUserRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/user/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ben", roles = {"USER"})
    void echo_WithAuth_ShouldReturnLdapEcho() throws Exception {
        Map<String, Object> mockResult = Map.of(
            "message", "test message",
            "user", Map.of("username", "ben", "authType", "LDAP"),
            "ldapServer", Map.of("connected", true)
        );
        
        when(ldapDomainLogic.processLdapAuthentication("test message")).thenReturn(mockResult);

        mockMvc.perform(get("/api/user/echo").param("message", "test message"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("test message"));
    }

    @Test
    @WithMockUser(username = "ben", roles = {"USER"})
    void getUserAttributes_WithAuth_ShouldReturnAttributes() throws Exception {
        Map<String, Object> mockUser = Map.of(
            "username", "ben",
            "dn", "uid=ben,ou=people,dc=springframework,dc=org"
        );
        
        Map<String, Object> mockGroups = Map.of(
            "groups", java.util.List.of("users"),
            "roles", java.util.List.of("ROLE_USER")
        );
        
        Map<String, Object> mockServerStatus = Map.of(
            "connected", true,
            "serverUrl", "ldap://localhost:8389"
        );
        
        when(ldapDomainLogic.getCurrentUser()).thenReturn(mockUser);
        when(ldapDomainLogic.getUserGroups()).thenReturn(mockGroups);
        when(ldapDomainLogic.getLdapServerStatus()).thenReturn(mockServerStatus);

        mockMvc.perform(get("/api/user/attributes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userAttributes").exists())
                .andExpect(jsonPath("$.groupMembership").exists())
                .andExpect(jsonPath("$.ldapServer").exists());
    }
}