package fixtures.core.model;

import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.function.Supplier;

public class ApplicationFixture {

    private ApplicationFixture() {
    }

    public static final Supplier<Application.ApplicationBuilder> BASE = () ->
        Application
            .builder()
            .id("app-id")
            .name("Test App name")
            .description("Test App description")
            .environmentId("my-env")
            .groups(Set.of())
            .createdAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
            .status(ApplicationStatus.ACTIVE);

    public static Application anApplication() {
        return BASE.get().build();
    }
}
