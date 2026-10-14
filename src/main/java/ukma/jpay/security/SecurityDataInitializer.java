package ukma.jpay.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SecurityDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String sampleRawPassword = "secretPassword123";
        String sampleEncodedHash = passwordEncoder.encode(sampleRawPassword);
        log.info("Example generated password hash: raw='{}', hash='{}'", sampleRawPassword, sampleEncodedHash);

        if (!userRepository.existsByUsername("user")) {
            userRepository.save(new UserEntity("user", passwordEncoder.encode("user123"), Role.ROLE_USER));
        }

        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new UserEntity("admin", passwordEncoder.encode("admin123"), Role.ROLE_ADMIN));
        }
    }
}
