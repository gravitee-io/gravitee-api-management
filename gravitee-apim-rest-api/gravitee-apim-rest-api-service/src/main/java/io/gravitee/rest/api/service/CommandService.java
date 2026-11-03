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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandQuery;
import io.gravitee.rest.api.model.command.NewCommandEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.List;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface CommandService {
    void send(ExecutionContext executionContext, NewCommandEntity message);

    List<CommandEntity> search(CommandQuery query);

    List<CommandEntity> search(ExecutionContext executionContext, CommandQuery query);

    /**
     * @param after the last command of the previous page, or {@code null} for the first page
     * @param limit the maximum number of commands to return, at least 1
     * @return at most {@code limit} commands matching the query and created after {@code after}, oldest first
     */
    List<CommandEntity> search(CommandQuery query, CommandEntity after, int limit);

    void ack(String messageId);

    /**
     * Acknowledge a command for this node, telling whether it worked.
     * @return {@code true} if the command is acknowledged by this node, {@code false} if it could not be, because it
     * no longer exists or the acknowledgment failed
     */
    boolean ackIfExists(String commandId);
    void delete(String commandId);

    /**
     * @return {@code true} only if this call removed the command, so that a single node processes it
     */
    boolean deleteIfExists(String commandId);

    int deleteByExpiredAtBefore(Instant before);
}
