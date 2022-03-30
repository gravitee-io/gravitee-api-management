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
package io.gravitee.rest.api.services.search;

import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.service.CommandService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledSearchIndexerServiceTest {

    @InjectMocks
    ScheduledSearchIndexerService service = new ScheduledSearchIndexerService();

    @Mock
    CommandService commandService;

    @Mock
    SearchEngineService searchEngineService;

    @Test
    public void shouldDoNothing() {
        when(commandService.search(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Collections.emptyList());

        service.run();

        verify(commandService, never()).ack(anyString());
        verify(searchEngineService, never()).process(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldInsertAndDelete() {
        CommandEntity insert = new CommandEntity();
        insert.setId("insertid");
        insert.setTags(Collections.singletonList(CommandTags.DATA_TO_INDEX));
        insert.setContent("{\"id\":\"1\"}");
        CommandEntity delete = new CommandEntity();
        delete.setId("deleteid");
        delete.setTags(Collections.singletonList(CommandTags.DATA_TO_INDEX));
        delete.setContent("{\"id\":\"2\"}");
        when(commandService.search(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Arrays.asList(delete, insert));

        service.run();

        verify(commandService, times(2)).ack(anyString());
        verify(searchEngineService, times(2)).process(eq(GraviteeContext.getExecutionContext()), any());
    }
}
