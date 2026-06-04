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
package io.gravitee.gamma.authorization.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryAuthzSchemaRepositoryTest {

    @Test
    void find_returns_empty_when_nothing_saved() {
        assertThat(new InMemoryAuthzSchemaRepository().find("env-1")).isEmpty();
    }

    @Test
    void save_then_find_returns_latest_per_environment() {
        InMemoryAuthzSchemaRepository repo = new InMemoryAuthzSchemaRepository();
        repo.save("env-1", "entity User {};", Instant.parse("2026-06-04T00:00:00Z"));
        repo.save("env-1", "entity User {}; entity Group {};", Instant.parse("2026-06-04T01:00:00Z"));
        repo.save("env-2", "entity API {};", Instant.parse("2026-06-04T00:00:00Z"));
        assertThat(repo.find("env-1")).contains("entity User {}; entity Group {};");
        assertThat(repo.find("env-2")).contains("entity API {};");
    }

    @Test
    void delete_removes_the_schema() {
        InMemoryAuthzSchemaRepository repo = new InMemoryAuthzSchemaRepository();
        repo.save("env-1", "entity User {};", Instant.parse("2026-06-04T00:00:00Z"));
        assertThat(repo.delete("env-1")).isTrue();
        assertThat(repo.find("env-1")).isEmpty();
        assertThat(repo.delete("env-1")).isFalse();
    }
}
