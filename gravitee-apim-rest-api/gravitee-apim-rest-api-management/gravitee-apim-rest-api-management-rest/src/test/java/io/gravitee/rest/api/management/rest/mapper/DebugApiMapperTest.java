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
package io.gravitee.rest.api.management.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.debug.DebugApi;
import io.gravitee.rest.api.model.DebugApiEntity;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DebugApiMapperTest {

    @Test
    void test() {
        DebugApi debugApi = DebugApiMapper.INSTANCE.fromEntity(
            DebugApiEntity.builder().id("api-id").name("api-name").plans(Set.of()).build()
        );
        assertThat(debugApi).isNotNull();
        assertThat(debugApi.getId()).isEqualTo("api-id");
        assertThat(debugApi.getName()).isEqualTo("api-name");
    }
}
