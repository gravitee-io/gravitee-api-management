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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AccessPointMapper {

    public ReactableAccessPoint to(io.gravitee.repository.management.model.AccessPoint accessPointModel) {
        // At this point we know that the access point is only referencing a Environment
        return ReactableAccessPoint
            .builder()
            .id(accessPointModel.getId())
            .environmentId(accessPointModel.getReferenceId())
            .host(accessPointModel.getHost())
            .target(accessPointModel.getTarget() != null ? ReactableAccessPoint.Target.valueOf(accessPointModel.getTarget().name()) : null)
            .build();
    }
}
