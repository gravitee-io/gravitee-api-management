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
package io.gravitee.gateway.services.sync.process.distributed.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SyncActionMapperTest {

    private static Stream<Arguments> provideSyncActions() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(SyncAction.DEPLOY, DistributedSyncAction.DEPLOY),
            Arguments.of(SyncAction.UNDEPLOY, DistributedSyncAction.UNDEPLOY)
        );
    }

    @ParameterizedTest
    @MethodSource("provideSyncActions")
    void should_return_syncAction_from_distributedSyncAction(SyncAction syncAction, DistributedSyncAction distributedSyncAction) {
        assertThat(SyncActionMapper.to(syncAction)).isEqualTo(distributedSyncAction);
    }

    @ParameterizedTest
    @MethodSource("provideSyncActions")
    void should_return_distributedSyncAction_from_syncAction(SyncAction syncAction, DistributedSyncAction distributedSyncAction) {
        assertThat(SyncActionMapper.to(distributedSyncAction)).isEqualTo(syncAction);
    }
}
