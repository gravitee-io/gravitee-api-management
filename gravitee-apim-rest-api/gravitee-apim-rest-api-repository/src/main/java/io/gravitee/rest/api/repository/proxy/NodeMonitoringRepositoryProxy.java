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
package io.gravitee.rest.api.repository.proxy;

import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class NodeMonitoringRepositoryProxy extends AbstractProxy<NodeMonitoringRepository> implements NodeMonitoringRepository {

    public Maybe<Monitoring> findByNodeIdAndType(String nodeId, String type) {
        return target.findByNodeIdAndType(nodeId, type);
    }

    public Single<Monitoring> create(Monitoring monitoring) {
        return target.create(monitoring);
    }

    public Single<Monitoring> update(Monitoring monitoring) {
        return target.update(monitoring);
    }

    public Flowable<Monitoring> findByTypeAndTimeFrame(String type, long from, long to) {
        return target.findByTypeAndTimeFrame(type, from, to);
    }
}
