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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpEventLatestRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private EventLatestRepository cut;

    @Test
    public void search() {
        List<Event> events = cut.search(EventCriteria.builder().build(), Event.EventProperties.API_ID, 10L, 2L);

        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    public void createOrUpdate() throws TechnicalException {
        Event event = cut.createOrUpdate(new Event());

        assertNull(event);
    }

}
