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
package fixtures.repository;

import io.gravitee.repository.management.model.User;
import java.time.Instant;
import java.util.Date;

public class UserFixtures {

    private UserFixtures() {}

    public static User aRepositoryUser() {
        return baseBuilder("user-id", "source", "source-id", "jane.doe@gravitee.io", "Jane", "Doe").build();
    }

    public static User aRepositoryUser(String id, String source, String sourceId, String email, String firstname, String lastname) {
        return baseBuilder(id, source, sourceId, email, firstname, lastname).build();
    }

    private static User.UserBuilder baseBuilder(
        String id,
        String source,
        String sourceId,
        String email,
        String firstname,
        String lastname
    ) {
        return User.builder()
            .id(id)
            .organizationId("organization-id")
            .source(source)
            .sourceId(sourceId)
            .email(email)
            .firstname(firstname)
            .lastname(lastname)
            .createdAt(Date.from(Instant.parse("2020-01-01T00:00:00Z")))
            .updatedAt(Date.from(Instant.parse("2020-01-02T00:00:00Z")));
    }
}
