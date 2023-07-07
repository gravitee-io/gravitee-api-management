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
package io.gravitee.repository.noop;

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.monitoring.MonitoringRepository;
import io.gravitee.repository.monitoring.model.MonitoringResponse;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpMonitoringRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Test
    public void testQuery() throws AnalyticsException, IOException {
        Assert.assertNotNull(monitoringRepository);

        final MonitoringResponse monitoringResponse = monitoringRepository.query("1876c024-c6a2-409a-b6c0-24c6a2e09a5f");

        Assert.assertNull(monitoringResponse);
    }
}
