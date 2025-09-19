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
package io.gravitee.gateway.services.sync.process.distributed.mapper;

import io.gravitee.gateway.services.sync.process.repository.synchronizer.license.LicenseDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class LicenseMapper {

    public Maybe<LicenseDeployable> to(final DistributedEvent event) {
        return Maybe.just(
            LicenseDeployable.builder()
                .id(event.getId())
                .license(event.getPayload())
                .syncAction(SyncActionMapper.to(event.getSyncAction()))
                .build()
        );
    }

    public Maybe<DistributedEvent> to(final LicenseDeployable deployable) {
        return Maybe.just(
            DistributedEvent.builder()
                .payload(deployable.license())
                .id(deployable.id())
                .type(DistributedEventType.LICENSE)
                .syncAction(SyncActionMapper.to(deployable.syncAction()))
                .updatedAt(new Date())
                .build()
        );
    }
}
