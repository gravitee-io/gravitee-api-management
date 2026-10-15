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
package io.gravitee.apim.core.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.permissions.RolePermission;
import org.junit.jupiter.api.Test;

class ApiDataKindTest {

    /** The platform permission opens figures, never the requests themselves: this is what keeps log access per API. */
    @Test
    void should_never_open_the_requests_with_the_platform_permission() {
        assertThat(ApiDataKind.PER_REQUEST.isOpenedByPlatformPermission()).isFalse();
    }

    @Test
    void should_open_the_aggregates_with_the_platform_permission() {
        assertThat(ApiDataKind.AGGREGATES.isOpenedByPlatformPermission()).isTrue();
    }

    @Test
    void should_ask_the_analytics_permission_for_aggregates_and_the_log_permission_for_requests() {
        assertThat(ApiDataKind.AGGREGATES.getApiPermission()).isEqualTo(RolePermission.API_ANALYTICS);
        assertThat(ApiDataKind.PER_REQUEST.getApiPermission()).isEqualTo(RolePermission.API_LOG);
    }
}
