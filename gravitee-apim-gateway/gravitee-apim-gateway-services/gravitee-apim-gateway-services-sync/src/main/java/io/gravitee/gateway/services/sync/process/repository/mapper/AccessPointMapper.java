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

import io.gravitee.gateway.handlers.accesspoint.model.AccessPoint;
import io.gravitee.gateway.handlers.accesspoint.model.AccessPointReferenceType;
import io.gravitee.gateway.handlers.accesspoint.model.AccessPointStatus;
import io.gravitee.gateway.handlers.accesspoint.model.AccessPointTarget;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AccessPointMapper {

    public AccessPoint to(io.gravitee.repository.management.model.AccessPoint accessPointModel) {
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setId(accessPointModel.getId());
        accessPoint.setReferenceId(accessPointModel.getReferenceId());
        accessPoint.setHost(accessPointModel.getHost());
        accessPoint.setSecured(accessPointModel.isSecured());
        accessPoint.setOverriding(accessPointModel.isOverriding());
        accessPoint.setUpdatedAt(accessPointModel.getUpdatedAt());
        if (accessPointModel.getTarget() != null) {
            accessPoint.setTarget(AccessPointTarget.valueOf(accessPointModel.getTarget().name()));
        }
        if (accessPointModel.getReferenceType() != null) {
            accessPoint.setReferenceType(AccessPointReferenceType.valueOf(accessPointModel.getReferenceType().name()));
        }
        if (accessPointModel.getStatus() != null) {
            accessPoint.setStatus(AccessPointStatus.valueOf(accessPointModel.getStatus().name()));
        }
        return accessPoint;
    }
}
