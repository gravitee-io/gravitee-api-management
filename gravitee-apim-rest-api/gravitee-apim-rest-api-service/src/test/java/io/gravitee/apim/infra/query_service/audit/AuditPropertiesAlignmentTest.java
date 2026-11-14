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
package io.gravitee.apim.infra.query_service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.repository.management.model.Audit;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AuditPropertiesAlignmentTest {

    @Test
    void should_read_back_every_audit_property_the_core_writes() {
        assertThat(Arrays.stream(AuditProperties.values()).map(Enum::name))
            .as("audit properties written by the core but unknown to the repository model")
            .isSubsetOf(Arrays.stream(Audit.AuditProperties.values()).map(Enum::name).toList());
    }
}
