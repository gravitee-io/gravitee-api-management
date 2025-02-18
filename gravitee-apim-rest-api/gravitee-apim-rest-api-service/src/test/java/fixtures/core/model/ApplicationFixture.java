/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fixtures.core.model;

import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.function.Supplier;

public class ApplicationFixture {

    private ApplicationFixture() {}

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
