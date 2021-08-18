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
package io.gravitee.rest.api.service;

import static org.junit.Assert.*;

import io.gravitee.rest.api.model.InstanceListItem;
import io.gravitee.rest.api.model.InstanceState;
import io.gravitee.rest.api.service.impl.InstanceServiceImpl;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InstanceServiceTest {

    @Test
    public void expirePredicateShouldFilterOldUnknownState() {
        InstanceServiceImpl.ExpiredPredicate predicateDays = new InstanceServiceImpl.ExpiredPredicate(Duration.ofDays(7));
        InstanceServiceImpl.ExpiredPredicate predicateSeconds = new InstanceServiceImpl.ExpiredPredicate(Duration.ofSeconds(7 * 24 * 3600));

        InstanceListItem itemStarted = new InstanceListItem();
        itemStarted.setId("ok-1");
        itemStarted.setState(InstanceState.STARTED);
        itemStarted.setLastHeartbeatAt(new Date(Instant.now().toEpochMilli()));
        InstanceListItem itemStopped = new InstanceListItem();
        itemStopped.setId("ok-2");
        itemStopped.setState(InstanceState.STOPPED);
        itemStopped.setLastHeartbeatAt(new Date(Instant.now().minus(8, ChronoUnit.DAYS).toEpochMilli()));
        InstanceListItem itemUnknownVisible = new InstanceListItem();
        itemUnknownVisible.setId("ok-3");
        itemUnknownVisible.setState(InstanceState.UNKNOWN);
        itemUnknownVisible.setLastHeartbeatAt(new Date(Instant.now().minus(6, ChronoUnit.DAYS).toEpochMilli()));
        InstanceListItem itemUnknownNotVisible = new InstanceListItem();
        itemUnknownNotVisible.setId("ko-4");
        itemUnknownNotVisible.setState(InstanceState.UNKNOWN);
        itemUnknownNotVisible.setLastHeartbeatAt(new Date(Instant.now().minus(8, ChronoUnit.DAYS).toEpochMilli()));

        execFiltering(predicateDays, Stream.of(itemStarted, itemUnknownNotVisible, itemStopped, itemUnknownVisible));
        execFiltering(predicateSeconds, Stream.of(itemStarted, itemUnknownNotVisible, itemStopped, itemUnknownVisible));
    }

    private void execFiltering(InstanceServiceImpl.ExpiredPredicate predicateDays, Stream<InstanceListItem> stream) {
        List<InstanceListItem> items = stream.filter(predicateDays).collect(Collectors.toList());
        assertNotNull(items);
        assertEquals(3, items.size());
        for (InstanceListItem item : items) {
            assertFalse("ko-4".equals(item.getId()));
        }
    }
}
