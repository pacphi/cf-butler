package io.pivotal.cfapp.domain;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class PoliciesValidatorTest {

    private final PoliciesValidator policiesValidator;

    @Autowired
    public PoliciesValidatorTest(PoliciesValidator policiesValidator) {
        this.policiesValidator = policiesValidator;
    }

    @Test
    public void assertValidApplicationPolicy() {
        Map<String, Object> options = new HashMap<>();
        options.put("from-duration", "PT30S");
        ApplicationPolicy durationPolicy =
            ApplicationPolicy
                .builder()
                .description("Delete applications in stopped state that were pushed more than 30s ago.")
                .operation(ApplicationOperation.DELETE.getName())
                .organizationWhiteList(Set.of("zoo-labs"))
                .options(options)
                .state(ApplicationState.STOPPED.getName())
                .pk(100L)
                .id(null)
                .build();
        Assertions.assertThat(policiesValidator.validate(durationPolicy) == true);

        ApplicationPolicy noTimeframePolicy =
            ApplicationPolicy
                .builder()
                .description("Delete all applications in stopped state.")
                .operation(ApplicationOperation.DELETE.getName())
                .organizationWhiteList(Set.of("zoo-labs"))
                .state(ApplicationState.STOPPED.getName())
                .pk(100L)
                .id(null)
                .build();
        Assertions.assertThat(policiesValidator.validate(noTimeframePolicy) == true);

        Map<String, Object> options2 = new HashMap<>();
        options.put("from-datetime", LocalDateTime.now().minusDays(2));
        ApplicationPolicy dateTimePolicy =
            ApplicationPolicy
                .builder()
                .description("Delete all applications in stopped state that were pushed after date/time.")
                .operation(ApplicationOperation.DELETE.getName())
                .options(options2)
                .organizationWhiteList(Set.of("zoo-labs"))
                .state(ApplicationState.STOPPED.getName())
                .pk(100L)
                .id(null)
                .build();
        Assertions.assertThat(policiesValidator.validate(dateTimePolicy) == true);

        Map<String, Object> options3 = new HashMap<>();
        options.put("instances-from", 1);
        options.put("instances-to", 2);
        ApplicationPolicy scalingPolicy =
            ApplicationPolicy
                .builder()
                .description("Scale all applications ")
                .operation(ApplicationOperation.SCALE_INSTANCES.getName())
                .options(options3)
                .organizationWhiteList(Set.of("zoo-labs"))
                .state(ApplicationState.STOPPED.getName())
                .pk(100L)
                .id(null)
                .build();
        Assertions.assertThat(policiesValidator.validate(scalingPolicy) == true);
    }

    @Test
    public void assertInvalidApplicationPolicy() {
        Map<String, Object> options = new HashMap<>();
        options.put("from-duration", "PT30S");
        options.put("from-datetime", LocalDateTime.now().minusSeconds(30));
        ApplicationPolicy invalidDeletePolicy =
            ApplicationPolicy
                .builder()
                .description("Delete applications in stopped state that were pushed more than 30s ago, but with multiple timeframes.")
                .operation(ApplicationOperation.DELETE.getName())
                .organizationWhiteList(Set.of("zoo-labs"))
                .options(options)
                .state(ApplicationState.STOPPED.getName())
                .pk(100L)
                .id(null)
                .build();
        Assertions.assertThat(policiesValidator.validate(invalidDeletePolicy) == false);

        ApplicationPolicy invalidScalingPolicy =
            ApplicationPolicy
                .builder()
                .description("Scale all applications, no parameters supplied")
                .operation(ApplicationOperation.SCALE_INSTANCES.getName())
                .organizationWhiteList(Set.of("zoo-labs"))
                .state(ApplicationState.STOPPED.getName())
                .pk(100L)
                .id(null)
                .build();
        Assertions.assertThat(policiesValidator.validate(invalidScalingPolicy) == false);
    }
}