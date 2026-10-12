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
package io.gravitee.apim.rest.api.mcp.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class McpServerConfigurationTest {

    @Test
    void should_read_the_scopes_of_the_mount_case_and_separator_ignored() {
        assertThat(McpServerConfiguration.scopes("read")).containsExactly("read");
        assertThat(McpServerConfiguration.scopes("read, Write")).containsExactlyInAnyOrder("read", "write");
        assertThat(McpServerConfiguration.scopes("read write")).containsExactlyInAnyOrder("read", "write");
        assertThat(McpServerConfiguration.scopes("")).isEmpty();
        assertThat(McpServerConfiguration.scopes(null)).isEmpty();
    }
}
