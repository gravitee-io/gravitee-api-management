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
package io.gravitee.gateway.debug.definition;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.debug.utils.Stubs;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugApiTest {

    public static final String EVENT_ID = "evt-id";

    @Test
    public void shouldConstructAndComputeNewPath() throws Exception {
        final io.gravitee.definition.model.debug.DebugApi debugApi = Stubs.getADebugApiDefinition();

        final DebugApi result = new DebugApi(EVENT_ID, debugApi);

        assertThat(result.getProxy().getVirtualHosts()).hasSize(3);
        assertThat(result.getProxy().getVirtualHosts())
            .anyMatch(item -> item.getPath().equals("/" + EVENT_ID + "-path1"))
            .anyMatch(item -> item.getPath().equals("/" + EVENT_ID + "-path2"))
            .anyMatch(item -> item.getPath().equals("/" + EVENT_ID + "-path3"));
    }
}
