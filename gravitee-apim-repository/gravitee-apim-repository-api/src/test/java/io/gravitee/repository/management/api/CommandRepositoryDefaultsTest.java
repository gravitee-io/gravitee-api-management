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
package io.gravitee.repository.management.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.api.search.CommandCursor;
import io.gravitee.repository.management.model.Command;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Repositories that predate the bounded search and the claiming delete only implement the abstract methods, so the
 * default methods have to give them the same results, however inefficiently.
 */
class CommandRepositoryDefaultsTest {

    private final InMemoryCommandRepository repository = new InMemoryCommandRepository();

    @Test
    void should_return_the_oldest_commands_up_to_the_page_size() {
        repository.create(command("third", 3_000L));
        repository.create(command("first-b", 1_000L));
        repository.create(command("second", 2_000L));
        repository.create(command("first-a", 1_000L));

        List<Command> commands = repository.searchAfter(new CommandCriteria.Builder().build(), null, 3);

        assertThat(commands).extracting(Command::getId).containsExactly("first-a", "first-b", "second");
    }

    @Test
    void should_return_the_commands_strictly_after_the_cursor() {
        repository.create(command("third", 3_000L));
        repository.create(command("first-b", 1_000L));
        repository.create(command("second", 2_000L));
        repository.create(command("first-a", 1_000L));

        List<Command> commands = repository.searchAfter(new CommandCriteria.Builder().build(), new CommandCursor(1_000L, "first-a"), 2);

        assertThat(commands).extracting(Command::getId).containsExactly("first-b", "second");
    }

    @Test
    void should_reject_a_page_size_below_one() {
        assertThatThrownBy(() -> repository.searchAfter(new CommandCriteria.Builder().build(), null, 0)).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    void should_report_that_only_the_first_delete_removed_the_command() throws Exception {
        repository.create(command("command", 1_000L));

        assertThat(repository.deleteIfExists("command")).isTrue();
        assertThat(repository.deleteIfExists("command")).isFalse();
        assertThat(repository.findById("command")).isEmpty();
    }

    private static Command command(String id, long createdAt) {
        Command command = new Command();
        command.setId(id);
        command.setCreatedAt(new Date(createdAt));
        return command;
    }

    private static class InMemoryCommandRepository implements CommandRepository {

        private final Map<String, Command> commands = new LinkedHashMap<>();

        @Override
        public List<Command> search(CommandCriteria criteria) {
            return List.copyOf(commands.values());
        }

        @Override
        public List<String> deleteByEnvironmentId(String environmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> deleteByOrganizationId(String organizationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteByExpiredAtBefore(Instant before) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Command> findById(String id) {
            return Optional.ofNullable(commands.get(id));
        }

        @Override
        public Command create(Command item) {
            commands.put(item.getId(), item);
            return item;
        }

        @Override
        public Command update(Command item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String id) {
            commands.remove(id);
        }

        @Override
        public Set<Command> findAll() {
            return new HashSet<>(commands.values());
        }
    }
}
