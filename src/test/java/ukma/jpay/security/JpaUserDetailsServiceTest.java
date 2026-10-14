package ukma.jpay.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaUserDetailsServiceTest {

    private UserRepository userRepository;
    private JpaUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new JpaUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsernameSuccess() {
        UserEntity user = new UserEntity("john", "encoded-pass", Role.ROLE_USER);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("john");

        assertThat(userDetails.getUsername()).isEqualTo("john");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-pass");
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsernameNotFoundThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
