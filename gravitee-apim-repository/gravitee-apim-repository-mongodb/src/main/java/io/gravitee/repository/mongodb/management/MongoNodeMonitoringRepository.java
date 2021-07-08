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
package io.gravitee.repository.mongodb.management;

import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.mongodb.management.internal.model.MonitoringMongo;
import io.gravitee.repository.mongodb.management.internal.model.PlanMongo;
import io.gravitee.repository.mongodb.management.internal.node.NodeMonitoringMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoNodeMonitoringRepository implements NodeMonitoringRepository {

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private NodeMonitoringMongoRepository internalNodeMonitoringRepository;

    @Override
    public Maybe<Monitoring> findByNodeIdAndType(String nodeId, String type) {
        MonitoringMongo monitoring = internalNodeMonitoringRepository.findByNodeIdAndType(nodeId, type);
        return monitoring != null ? Maybe.just(map(monitoring)) : Maybe.empty();
    }

    @Override
    public Single<Monitoring> create(Monitoring monitoring) {
        MonitoringMongo monitoringMongo = internalNodeMonitoringRepository.insert(map(monitoring));
        return Single.just(map(monitoringMongo));
    }

    @Override
    public Single<Monitoring> update(Monitoring monitoring) {
        if (monitoring == null || monitoring.getId() == null) {
            return Single.error(new IllegalStateException("Node monitoring to update must have an id"));
        }

        MonitoringMongo monitoringMongo = internalNodeMonitoringRepository.findById(monitoring.getId()).orElse(null);

        if (monitoringMongo == null) {
            return Single.error(new IllegalStateException(String.format("No node monitoring found with id [%s]", monitoring.getId())));
        }

        monitoringMongo = map(monitoring);
        monitoringMongo = internalNodeMonitoringRepository.save(monitoringMongo);
        return Single.just(map(monitoringMongo));
    }

    @Override
    public Flowable<Monitoring> findByTypeAndTimeFrame(String type, long from, long to) {
        return Flowable.fromIterable(
            internalNodeMonitoringRepository.findByTypeAndTimeFrame(type, from, to).stream().map(this::map).collect(Collectors.toList())
        );
    }

    private MonitoringMongo map(Monitoring item) {
        return (item == null) ? null : mapper.map(item, MonitoringMongo.class);
    }

    private Monitoring map(MonitoringMongo item) {
        return (item == null) ? null : mapper.map(item, Monitoring.class);
    }
}
