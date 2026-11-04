package ukma.jpay.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
    }

    @Test
    void generatesAndValidatesToken() {
        UserDetails user = new User("alice", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("alice");

        boolean valid = jwtService.isTokenValid(token, user);
        assertThat(valid).isTrue();
    }

    @Test
    void rejectsTokenForDifferentUser() {
        UserDetails user1 = new User("alice", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UserDetails user2 = new User("bob", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtService.generateToken(user1);
        boolean valid = jwtService.isTokenValid(token, user2);

        assertThat(valid).isFalse();
    }
}
