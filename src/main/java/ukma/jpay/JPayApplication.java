package ukma.jpay;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "JPay API", version = "v1",
        description = "Створення платежів, отримання стану та приймання подій платіжних провайдерів"))
public class JPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(JPayApplication.class, args);
    }

}
