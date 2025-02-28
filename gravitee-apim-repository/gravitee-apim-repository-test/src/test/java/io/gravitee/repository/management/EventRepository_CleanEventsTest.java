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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.not;

import io.gravitee.repository.management.api.EventRepository;
import java.util.Comparator;
import org.assertj.core.api.Condition;
import org.junit.Test;

public class EventRepository_CleanEventsTest extends AbstractManagementRepositoryTest {

    // for the test the event from DEFAULT env don’t start with _ and others does
    static final Condition<EventRepository.EventToClean> DEFAULT_ENV = new Condition<>(
        e -> !e.id().startsWith("_"),
        "event from \"NOT-DEFAULT\" environment"
    );

    @Override
    protected String getTestCasesPath() {
        return "/data/event-cleaning/";
    }

    @Test
    public void findGatewayEvents() {
        // when
        var events = eventRepository.findGatewayEvents("DEFAULT").toList();

        // then
        assertThat(events)
            .doNotHave(not(DEFAULT_ENV))
            .filteredOn(DEFAULT_ENV)
            .hasSize(5)
            // to help the test, the dataset is created with order of creation date same as order of ids
            .isSortedAccordingTo(Comparator.comparing(EventRepository.EventToClean::id).reversed());
    }
}
