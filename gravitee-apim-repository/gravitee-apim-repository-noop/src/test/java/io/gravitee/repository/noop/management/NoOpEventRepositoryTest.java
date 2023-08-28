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
package io.gravitee.repository.noop.management;

import static org.junit.Assert.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpEventRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private EventRepository cut;

    @Test
    public void pageableSearch() {
        Page<Event> events = cut.search(EventCriteria.builder().build(), new PageableBuilder().build());

        assertNotNull(events);
        assertNotNull(events.getContent());
        assertTrue(events.getContent().isEmpty());
    }

    @Test
    public void search() {
        List<Event> events = cut.search(EventCriteria.builder().build());

        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    public void createOrPatch() throws TechnicalException {
        Event event = cut.createOrPatch(new Event());

        assertNull(event);
    }
}
