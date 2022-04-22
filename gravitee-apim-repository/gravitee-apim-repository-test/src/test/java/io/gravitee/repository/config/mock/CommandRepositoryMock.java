/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.config.mock;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.model.Command;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CommandRepositoryMock extends AbstractRepositoryMock<CommandRepository> {

    public CommandRepositoryMock() {
        super(CommandRepository.class);
    }

    @Override
    void prepare(CommandRepository commandRepository) throws Exception {
        //shouldCreate
        Command newCommand = new Command();
        newCommand.setId("msg-to-create");
        newCommand.setEnvironmentId("DEFAULT");
        newCommand.setOrganizationId("DEFAULT");
        newCommand.setTo("someone");
        newCommand.setTags(Arrays.asList("INSERT", "DATA_TO_INDEX"));
        newCommand.setContent("Hello, is it me you're looking for?");
        newCommand.setAcknowledgments(Arrays.asList("1", "a"));
        newCommand.setCreatedAt(new Date(1546305346000L));
        newCommand.setUpdatedAt(new Date(1548983746000L));
        newCommand.setExpiredAt(new Date(1551402946000L));
        when(commandRepository.findById("msg-to-create")).thenReturn(of(newCommand));

        //shouldUpdate
        Command updatedCommand = new Command();
        updatedCommand.setId("msg-to-update");
        updatedCommand.setOrganizationId("NEW_DEFAULT");
        updatedCommand.setEnvironmentId("new_DEFAULT");
        updatedCommand.setTo("message updated");
        updatedCommand.setFrom("from updated");
        updatedCommand.setTags(singletonList("DELETE"));
        updatedCommand.setContent("updated content");
        updatedCommand.setAcknowledgments(Arrays.asList("up", "date"));
        updatedCommand.setCreatedAt(new Date(1546405346000L));
        updatedCommand.setUpdatedAt(new Date(1546505346000L));
        updatedCommand.setExpiredAt(new Date(1546605346000L));
        when(commandRepository.update(argThat(o -> o != null && o.getId().equals("msg-to-update")))).thenReturn(updatedCommand);

        //shouldNotUpdateUnknownMessage
        when(commandRepository.update(argThat(o -> o != null && o.getId() == null))).thenThrow(new IllegalStateException());

        //shouldNotUpdateNull
        when(commandRepository.update(argThat(Objects::isNull))).thenThrow(new IllegalStateException());

        //shouldDelete
        when(commandRepository.findById("msg-to-delete")).thenReturn(of(mock(Command.class)), empty());

        //search
        Command search1 = new Command();
        search1.setId("search1");
        Command search2 = new Command();
        search2.setId("search2");
        Command search3 = new Command();
        search3.setId("search3");
        when(commandRepository.search(argThat(o -> o != null && o.getNotFrom() != null)))
            .thenReturn(Arrays.asList(newCommand, updatedCommand, mock(Command.class), search2, search3));
        when(commandRepository.search(argThat(o -> o != null && o.getTo() != null))).thenReturn(singletonList(search3));
        when(commandRepository.search(argThat(o -> o != null && o.getTags() != null && o.getTags().length == 1)))
            .thenReturn(Arrays.asList(newCommand, updatedCommand, search1, search2));
        when(commandRepository.search(argThat(o -> o != null && o.getTags() != null && o.getTags().length == 2)))
            .thenReturn(singletonList(search3));
        when(commandRepository.search(argThat(o -> o != null && o.getNotAckBy() != null)))
            .thenReturn(Arrays.asList(newCommand, updatedCommand, mock(Command.class), search2));
        when(commandRepository.search(argThat(o -> o != null && o.isNotExpired()))).thenReturn(singletonList(search2));
        when(commandRepository.search(argThat(o -> o != null && "DEFAULT".equals(o.getEnvironmentId()))))
            .thenReturn(singletonList(newCommand));
        when(commandRepository.search(argThat(o -> o != null && "DEFAULT".equals(o.getOrganizationId()))))
            .thenReturn(singletonList(newCommand));
    }
}
