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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.license.LicenseDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class LicenseMapperTest {

    private LicenseMapper cut;
    private DistributedEvent distributedEvent;
    private LicenseDeployable licenseDeployable;

    @BeforeEach
    public void beforeEach() throws JsonProcessingException {
        cut = new LicenseMapper();

        var id = "id";
        var content = "license-content";

        distributedEvent = DistributedEvent.builder()
            .id(id)
            .payload(content)
            .updatedAt(new Date())
            .type(DistributedEventType.LICENSE)
            .syncAction(DistributedSyncAction.DEPLOY)
            .build();

        licenseDeployable = LicenseDeployable.builder().id(id).license(content).syncAction(SyncAction.DEPLOY).build();
    }

    @Test
    void should_return_distributed_event() {
        cut
            .to(distributedEvent)
            .test()
            .assertValue(deployable -> {
                assertThat(deployable).isEqualTo(licenseDeployable);
                return true;
            });
    }

    @Test
    void should_map_license_deployable() {
        cut
            .to(licenseDeployable)
            .test()
            .assertValue(distributedEvent -> {
                assertThat(distributedEvent.getId()).isEqualTo(this.distributedEvent.getId());
                assertThat(distributedEvent.getType()).isEqualTo(this.distributedEvent.getType());
                assertThat(distributedEvent.getPayload()).isEqualTo(this.distributedEvent.getPayload());
                assertThat(distributedEvent.getSyncAction()).isEqualTo(this.distributedEvent.getSyncAction());
                return true;
            });
    }
}
