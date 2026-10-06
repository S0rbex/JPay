package ukma.jpay.shared.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("jpay.shared")
public record SharedModuleProperties(
        @DefaultValue("true") boolean enabled,
        @Valid @DefaultValue Notifications notifications) {

    public record Notifications(
            @DefaultValue("true") boolean enabled,
            @NotBlank @DefaultValue("jpay") String source) {
    }
}
