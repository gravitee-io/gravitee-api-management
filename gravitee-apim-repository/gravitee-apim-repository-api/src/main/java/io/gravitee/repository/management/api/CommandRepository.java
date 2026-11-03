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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.api.search.CommandCursor;
import io.gravitee.repository.management.model.Command;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface CommandRepository extends CrudRepository<Command, String> {
    List<Command> search(CommandCriteria criteria);

    /**
     * Keyset / seek pagination over the commands matching the criteria, sorted by {@code (createdAt ASC, id ASC)}.
     * The default implementation reads every matching command before keeping the page: implementations should
     * override it to bound the read. Where commands without a creation date sort depends on the implementation.
     * @param criteria the criteria commands must match, applied before the page size
     * @param after the position the page starts strictly after, or {@code null} to start from the oldest command
     * @param pageSize the maximum number of commands to return, at least 1
     * @return at most {@code pageSize} commands, oldest first. A short page means no more commands were found, or that
     * some were removed while the page was read: the next read starting after the last one returned finds the rest.
     */
    default List<Command> searchAfter(CommandCriteria criteria, CommandCursor after, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be at least 1, got " + pageSize);
        }
        Comparator<Command> order = Comparator.comparingLong(CommandRepository::createdAt).thenComparing(Command::getId);
        return search(criteria)
            .stream()
            .filter(
                command ->
                    after == null ||
                    createdAt(command) > after.createdAt() ||
                    (createdAt(command) == after.createdAt() && command.getId().compareTo(after.id()) > 0)
            )
            .sorted(order)
            .limit(pageSize)
            .toList();
    }

    private static long createdAt(Command command) {
        return command.getCreatedAt() == null ? Long.MIN_VALUE : command.getCreatedAt().getTime();
    }

    /**
     * Delete a command, telling whether this call is the one that removed it, so that only one of the callers racing
     * to delete the same command goes on to process it.
     * The default implementation is not atomic: implementations should override it.
     * @param id the command id
     * @return {@code true} if this call removed the command, {@code false} if it did not exist anymore
     */
    default boolean deleteIfExists(String id) throws TechnicalException {
        if (findById(id).isEmpty()) {
            return false;
        }
        delete(id);
        return true;
    }

    /**
     * Delete commands by environment
     * @param environmentId
     * @return List of IDs for deleted commands
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;

    /**
     * Delete commands by organization
     * @param organizationId
     * @return List of IDs for deleted commands
     */
    List<String> deleteByOrganizationId(String organizationId) throws TechnicalException;

    /**
     * Delete commands that expired before the given instant
     * @param before the cutoff instant
     * @return number of deleted commands
     */
    int deleteByExpiredAtBefore(Instant before) throws TechnicalException;
}
