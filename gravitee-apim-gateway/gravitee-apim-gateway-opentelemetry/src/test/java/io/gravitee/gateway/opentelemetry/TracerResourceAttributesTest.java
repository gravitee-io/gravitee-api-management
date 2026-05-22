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
package io.gravitee.gateway.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TracerResourceAttributesTest {

    @Test
    void should_emit_all_six_attributes_when_every_field_is_populated() {
        Map<String, String> attrs = TracerResourceAttributes.of("apim", "api-id", "api-name", "API_V4", "org-id", "env-id");

        // containsExactly enforces insertion order — useful because the LinkedHashMap order is what
        // shows up in OTel exporters that render attributes deterministically (Loki labels, Grafana UI).
        assertThat(attrs).containsExactly(
            Map.entry("gravitee.module", "apim"),
            Map.entry("gravitee.api.id", "api-id"),
            Map.entry("gravitee.api.name", "api-name"),
            Map.entry("gravitee.api.type", "API_V4"),
            Map.entry("gravitee.org.id", "org-id"),
            Map.entry("gravitee.env.id", "env-id")
        );
    }

    @Test
    void should_record_module_verbatim_so_each_reactor_owns_its_own_namespace() {
        // ESM, AIM and any future reactor pass their own module string — verbatim, no normalisation.
        assertThat(TracerResourceAttributes.of("esm", null, null, null, null, null)).containsEntry("gravitee.module", "esm");
        assertThat(TracerResourceAttributes.of("aim", null, null, null, null, null)).containsEntry("gravitee.module", "aim");
    }

    @Test
    void should_omit_api_id_entry_when_value_is_null() {
        Map<String, String> attrs = TracerResourceAttributes.of("apim", null, "api-name", "API_V4", "org-id", "env-id");

        assertThat(attrs).doesNotContainKey("gravitee.api.id").containsKeys("gravitee.api.name", "gravitee.api.type");
    }

    @Test
    void should_omit_api_name_entry_when_value_is_null() {
        Map<String, String> attrs = TracerResourceAttributes.of("apim", "api-id", null, "API_V4", "org-id", "env-id");

        assertThat(attrs).doesNotContainKey("gravitee.api.name").containsKeys("gravitee.api.id", "gravitee.api.type");
    }

    @Test
    void should_omit_api_type_entry_when_value_is_null() {
        Map<String, String> attrs = TracerResourceAttributes.of("apim", "api-id", "api-name", null, "org-id", "env-id");

        assertThat(attrs).doesNotContainKey("gravitee.api.type").containsKeys("gravitee.api.id", "gravitee.api.name");
    }

    @Test
    void should_omit_org_id_entry_when_value_is_null() {
        // Legacy fixtures / minimal IT API definitions can ship with null organizationId — the
        // deployment must still succeed, with whatever identity is available.
        Map<String, String> attrs = TracerResourceAttributes.of("apim", "api-id", "api-name", "API_V4", null, "env-id");

        assertThat(attrs).doesNotContainKey("gravitee.org.id").containsEntry("gravitee.env.id", "env-id");
    }

    @Test
    void should_omit_env_id_entry_when_value_is_null() {
        Map<String, String> attrs = TracerResourceAttributes.of("apim", "api-id", "api-name", "API_V4", "org-id", null);

        assertThat(attrs).doesNotContainKey("gravitee.env.id").containsEntry("gravitee.org.id", "org-id");
    }

    @Test
    void should_keep_only_module_when_every_optional_field_is_null() {
        // Mirrors the minimal-definition path: a reactor that only knows its own module name and no
        // API identity at all (early bootstrap, fallback tracers) still gets a usable resource map.
        Map<String, String> attrs = TracerResourceAttributes.of("apim", null, null, null, null, null);

        assertThat(attrs).containsExactly(Map.entry("gravitee.module", "apim"));
    }

    @Test
    void should_reject_null_module_with_a_clear_npe() {
        // Module is the only non-optional field — the contract is documented in the Javadoc and now
        // enforced at the boundary so the failure surfaces with context instead of leaking into the
        // OTel exporter pipeline as a confusing downstream NPE.
        assertThatNullPointerException()
            .isThrownBy(() -> TracerResourceAttributes.of(null, "api-id", "api-name", "API_V4", "org-id", "env-id"))
            .withMessageContaining("module");
    }

    @Test
    void should_return_a_mutable_map_so_callers_can_extend_without_copy() {
        // The contract isn't immutable on purpose — downstream tracer builders may want to add their
        // own attributes (deployment-specific labels) without an intermediate copy. Use a put to check
        // the returned map allows mutation rather than throwing UnsupportedOperationException.
        Map<String, String> attrs = TracerResourceAttributes.of("apim", "api-id", null, null, null, null);

        attrs.put("custom.extra", "value");

        assertThat(attrs).containsEntry("custom.extra", "value");
    }
}
