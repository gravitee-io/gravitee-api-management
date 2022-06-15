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
package io.gravitee.repository.management;

import io.gravitee.node.api.Monitoring;
import io.gravitee.repository.config.AbstractManagementRepositoryTest;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeMonitoringRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/nodemonitoring-tests/";
    }

    @Override
    protected Class getClassFromFileName(String baseName) {
        return Monitoring.class;
    }

    @Test
    public void shouldFindByNodeIdAndType() {
        final TestObserver<Monitoring> testObserver = nodeMonitoringRepository.findByNodeIdAndType("nodeId1", Monitoring.NODE_INFOS).test();

        awaitTerminalEvent(testObserver);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(monitoring -> monitoring.getNodeId().equals("nodeId1"));
    }

    @Test
    public void shouldFindByUnknownNodeId() {
        final TestObserver<Monitoring> testObserver = nodeMonitoringRepository.findByNodeIdAndType("unknown", Monitoring.NODE_INFOS).test();

        awaitTerminalEvent(testObserver);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertResult();
    }

    @Test
    public void shouldFindByTypeAndTimeFrame() {
        final TestSubscriber<Monitoring> testObserver = nodeMonitoringRepository
            .findByTypeAndTimeFrame(Monitoring.HEALTH_CHECK, 1617753600000L, 1617926400000L)
            .test();

        testObserver.awaitTerminalEvent(15, TimeUnit.SECONDS);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValueCount(1);
    }

    @Test
    public void shouldCreate() {
        final Monitoring monitoring = new Monitoring();
        monitoring.setId("1");
        monitoring.setCreatedAt(new Date(1464739200000L));
        monitoring.setUpdatedAt(new Date(1464739200000L));
        monitoring.setEvaluatedAt(new Date(1464739200000L));
        monitoring.setPayload("{}");
        monitoring.setType(Monitoring.HEALTH_CHECK);
        monitoring.setNodeId("nodeId1");

        final TestObserver<Monitoring> testObserver = nodeMonitoringRepository.create(monitoring).test();

        awaitTerminalEvent(testObserver);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(createdMonitoring -> createdMonitoring.getNodeId().equals("nodeId1"));
    }

    @Test
    public void shouldUpdate() {
        final Monitoring monitoring = new Monitoring();
        monitoring.setId("nodeMonitoring1");
        monitoring.setCreatedAt(new Date(1000000000000L));
        monitoring.setUpdatedAt(new Date(1200000000000L));
        monitoring.setEvaluatedAt(new Date(1464739200000L));
        monitoring.setPayload("{}");
        monitoring.setType(Monitoring.NODE_INFOS);
        monitoring.setNodeId("nodeId2");

        final TestObserver<Monitoring> testObserver = nodeMonitoringRepository.update(monitoring).test();

        awaitTerminalEvent(testObserver);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(createdMonitoring -> createdMonitoring.getNodeId().equals("nodeId2"));
    }

    @Test
    public void shouldNotUpdateUnknownMonitoring() {
        final Monitoring monitoring = new Monitoring();
        monitoring.setId("unknown");

        final TestObserver<Monitoring> testObserver = nodeMonitoringRepository.update(monitoring).test();

        awaitTerminalEvent(testObserver);
        testObserver.assertNotComplete();
        testObserver.assertError(IllegalStateException.class);
    }

    private void awaitTerminalEvent(TestObserver<Monitoring> testObserver) {
        testObserver.awaitTerminalEvent(15, TimeUnit.SECONDS);
    }
}
