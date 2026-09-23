package ukma.jpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class JPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(JPayApplication.class, args);
    }

}
