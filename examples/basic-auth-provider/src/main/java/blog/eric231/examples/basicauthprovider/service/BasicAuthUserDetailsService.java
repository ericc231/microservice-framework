package blog.eric231.examples.basicauthprovider.service;

import blog.eric231.examples.basicauthprovider.domain.User;
import blog.eric231.examples.basicauthprovider.domain.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasicAuthUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public BasicAuthUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<String> roles = Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                roles.stream().map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role)).collect(Collectors.toList())
        );
    }
}
