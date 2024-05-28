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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.gravitee.repository.management.model.AccessPointTarget;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class AccessPointMapperTest {

    private AccessPointMapper cut;
    private io.gravitee.repository.management.model.AccessPoint accessPoint;

    @BeforeEach
    public void beforeEach() {
        cut = new AccessPointMapper();

        accessPoint = new AccessPoint();
        accessPoint.setId("id");
        accessPoint.setStatus(AccessPointStatus.CREATED);
        accessPoint.setTarget(AccessPointTarget.GATEWAY);
        accessPoint.setUpdatedAt(new Date());
        accessPoint.setReferenceId("referenceId");
        accessPoint.setReferenceType(AccessPointReferenceType.ENVIRONMENT);
        accessPoint.setHost("host");
        accessPoint.setSecured(true);
        accessPoint.setOverriding(true);
    }

    @Test
    void should_map_accesspoint() throws JsonProcessingException {
        io.gravitee.gateway.handlers.accesspoint.model.AccessPoint accessPointMapped = cut.to(accessPoint);

        assertThat(accessPointMapped.getId()).isEqualTo(accessPoint.getId());
        assertThat(accessPointMapped.getStatus()).isEqualTo(io.gravitee.gateway.handlers.accesspoint.model.AccessPointStatus.CREATED);
        assertThat(accessPointMapped.getTarget()).isEqualTo(io.gravitee.gateway.handlers.accesspoint.model.AccessPointTarget.GATEWAY);
        assertThat(accessPointMapped.getUpdatedAt()).isEqualTo(accessPoint.getUpdatedAt());
        assertThat(accessPointMapped.getReferenceId()).isEqualTo(accessPoint.getReferenceId());
        assertThat(accessPointMapped.getReferenceType())
            .isEqualTo(io.gravitee.gateway.handlers.accesspoint.model.AccessPointReferenceType.ENVIRONMENT);
        assertThat(accessPointMapped.getHost()).isEqualTo(accessPoint.getHost());
        assertThat(accessPointMapped.isSecured()).isEqualTo(accessPoint.isSecured());
        assertThat(accessPointMapped.isOverriding()).isEqualTo(accessPoint.isOverriding());
    }

    @Test
    void should_map_accesspoint_without_all_attributes() throws JsonProcessingException {
        accessPoint.setStatus(null);
        accessPoint.setTarget(null);
        accessPoint.setReferenceType(null);
        io.gravitee.gateway.handlers.accesspoint.model.AccessPoint accessPointMapped = cut.to(accessPoint);

        assertThat(accessPointMapped.getId()).isEqualTo(accessPoint.getId());
        assertThat(accessPointMapped.getStatus()).isNull();
        assertThat(accessPointMapped.getTarget()).isNull();
        assertThat(accessPointMapped.getUpdatedAt()).isEqualTo(accessPoint.getUpdatedAt());
        assertThat(accessPointMapped.getReferenceId()).isEqualTo(accessPoint.getReferenceId());
        assertThat(accessPointMapped.getReferenceType()).isNull();
        assertThat(accessPointMapped.getHost()).isEqualTo(accessPoint.getHost());
        assertThat(accessPointMapped.isSecured()).isEqualTo(accessPoint.isSecured());
        assertThat(accessPointMapped.isOverriding()).isEqualTo(accessPoint.isOverriding());
    }
}
