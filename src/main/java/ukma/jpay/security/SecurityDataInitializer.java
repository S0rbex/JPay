package ukma.jpay.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SecurityDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String userPassword;
    private final String adminPassword;

    public SecurityDataInitializer(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${jpay.security.user-password}") String userPassword,
                                   @Value("${jpay.security.admin-password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userPassword = userPassword;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        String sampleRawPassword = "secretPassword123";
        String sampleEncodedHash = passwordEncoder.encode(sampleRawPassword);
        log.info("Example generated password hash: raw='{}', hash='{}'", sampleRawPassword, sampleEncodedHash);

        if (!userRepository.existsByUsername("user")) {
            userRepository.save(new UserEntity("user", passwordEncoder.encode(userPassword), Role.ROLE_USER));
        }

        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new UserEntity("admin", passwordEncoder.encode(adminPassword), Role.ROLE_ADMIN));
        }
    }
}
