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
package io.gravitee.repository.mock.management;

import static io.gravitee.repository.utils.DateUtils.parse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Date;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeMonitoringRepositoryMock extends AbstractRepositoryMock<NodeMonitoringRepository> {

    public NodeMonitoringRepositoryMock() {
        super(NodeMonitoringRepository.class);
    }

    @Override
    protected void prepare(NodeMonitoringRepository nodeMonitoringRepository) throws Exception {
        // findByNodeIdAndType
        final Monitoring monitoringFound = mock(Monitoring.class);
        when(monitoringFound.getPayload()).thenReturn("{}");
        when(monitoringFound.getNodeId()).thenReturn("nodeId1");
        when(monitoringFound.getCreatedAt()).thenReturn(parse("06/04/2021"));
        when(monitoringFound.getUpdatedAt()).thenReturn(parse("06/04/2021"));
        when(monitoringFound.getEvaluatedAt()).thenReturn(parse("06/04/2021"));

        when(nodeMonitoringRepository.findByNodeIdAndType("nodeId1", Monitoring.NODE_INFOS)).thenReturn(Maybe.just(monitoringFound));

        // findByNodeIdUnknown
        when(nodeMonitoringRepository.findByNodeIdAndType("unknown", Monitoring.NODE_INFOS)).thenReturn(Maybe.empty());

        // findByTypeAndTimeFrame
        final Monitoring monitoringByDate = mock(Monitoring.class);
        when(monitoringByDate.getPayload()).thenReturn("{}");
        when(monitoringByDate.getId()).thenReturn("nodeMonitoring3");
        when(monitoringByDate.getNodeId()).thenReturn("nodeId1");
        when(monitoringByDate.getType()).thenReturn(Monitoring.HEALTH_CHECK);
        when(monitoringByDate.getCreatedAt()).thenReturn(parse("08/04/2021"));
        when(monitoringByDate.getUpdatedAt()).thenReturn(parse("08/04/2021"));
        when(monitoringByDate.getEvaluatedAt()).thenReturn(parse("08/04/2021"));
        when(nodeMonitoringRepository.findByTypeAndTimeFrame(Monitoring.HEALTH_CHECK, 1617753600000L, 1617926400000L))
            .thenReturn(Flowable.just(monitoringByDate));

        // create
        final Monitoring createMonitoring = new Monitoring();
        createMonitoring.setId("1");
        createMonitoring.setCreatedAt(new Date(1464739200000L));
        createMonitoring.setUpdatedAt(new Date(1464739200000L));
        createMonitoring.setEvaluatedAt(new Date(1464739200000L));
        createMonitoring.setPayload("{}");
        createMonitoring.setType(Monitoring.NODE_INFOS);
        createMonitoring.setNodeId("nodeId1");

        when(nodeMonitoringRepository.create(any())).thenReturn(Single.just(createMonitoring));

        // update
        final Monitoring updateMonitoring = new Monitoring();
        updateMonitoring.setId("nodeMonitoring1");
        updateMonitoring.setCreatedAt(new Date(1464739200000L));
        updateMonitoring.setUpdatedAt(new Date(1464739200000L));
        updateMonitoring.setEvaluatedAt(new Date(1464739200000L));
        updateMonitoring.setPayload("{}");
        updateMonitoring.setType(Monitoring.NODE_INFOS);
        updateMonitoring.setNodeId("nodeId2");

        when(nodeMonitoringRepository.update(argThat(o -> o != null && o.getId().equals("nodeMonitoring1"))))
            .thenReturn(Single.just(updateMonitoring));

        // updateUnknownMonitoring
        when(nodeMonitoringRepository.update(argThat(o -> o != null && o.getId().equals("unknown"))))
            .thenReturn(Single.error(new IllegalStateException()));
    }
}
