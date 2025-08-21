package blog.eric231.examples.basicauthprovider;

import blog.eric231.examples.basicauthprovider.domain.User;
import blog.eric231.examples.basicauthprovider.domain.UserRepository;
import blog.eric231.examples.basicauthprovider.service.BasicAuthUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicAuthUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private BasicAuthUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new BasicAuthUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("testpassword");
        user.setRoles("ROLE_USER, ROLE_ADMIN");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When
        UserDetails result = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("testpassword", result.getPassword());
        assertEquals(2, result.getAuthorities().size());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, 
            () -> userDetailsService.loadUserByUsername("nonexistent"));
    }

    @Test
    void loadUserByUsername_ShouldHandleSingleRole() {
        // Given
        User user = new User();
        user.setUsername("singleuser");
        user.setPassword("password");
        user.setRoles("ROLE_USER");

        when(userRepository.findByUsername("singleuser")).thenReturn(Optional.of(user));

        // When
        UserDetails result = userDetailsService.loadUserByUsername("singleuser");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_ShouldTrimRoles() {
        // Given
        User user = new User();
        user.setUsername("trimuser");
        user.setPassword("password");
        user.setRoles(" ROLE_USER ,  ROLE_ADMIN  ");

        when(userRepository.findByUsername("trimuser")).thenReturn(Optional.of(user));

        // When
        UserDetails result = userDetailsService.loadUserByUsername("trimuser");

        // Then
        assertNotNull(result);
        assertEquals(2, result.getAuthorities().size());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }
}