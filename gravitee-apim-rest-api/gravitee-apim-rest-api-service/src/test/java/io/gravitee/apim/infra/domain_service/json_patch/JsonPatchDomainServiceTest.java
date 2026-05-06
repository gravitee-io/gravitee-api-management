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
package io.gravitee.apim.infra.domain_service.json_patch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.json_patch.domain_service.JsonPatchDomainService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JsonPatchDomainServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonPatchDomainService cut = new JsonPatchDomainService(new JsonMergePatchServiceImpl(), new JsonPatchServiceImpl());

    @Test
    void applyJsonPatch_applies_replace_operation() throws Exception {
        var target = objectMapper.readTree("{\"name\":\"old\"}");
        var patch = objectMapper.readTree("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"new\"}]");

        var result = cut.applyJsonPatch(patch, target);

        assertThat(result.get("name").asText()).isEqualTo("new");
    }

    @Test
    void applyJsonPatch_wraps_failed_operation_in_validation_exception() throws Exception {
        var target = objectMapper.readTree("{}");
        var patch = objectMapper.readTree("[{\"op\":\"replace\",\"path\":\"/missing\",\"value\":\"x\"}]");

        assertThatThrownBy(() -> cut.applyJsonPatch(patch, target))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageStartingWith("Failed to apply patch:");
    }

    @Test
    void applyJsonPatch_wraps_malformed_patch_in_validation_exception() throws Exception {
        var target = objectMapper.readTree("{}");
        var patch = objectMapper.readTree("{\"not\":\"an array\"}");

        assertThatThrownBy(() -> cut.applyJsonPatch(patch, target))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageStartingWith("Invalid patch:");
    }

    @Test
    void applyMergePatch_applies_field_override() throws Exception {
        var target = objectMapper.readTree("{\"name\":\"old\",\"keep\":\"same\"}");
        var patch = objectMapper.readTree("{\"name\":\"new\"}");

        var result = cut.applyMergePatch(patch, target);

        assertThat(result.get("name").asText()).isEqualTo("new");
        assertThat(result.get("keep").asText()).isEqualTo("same");
    }

    @Test
    void applyMergePatch_null_value_removes_field() throws Exception {
        var target = objectMapper.readTree("{\"name\":\"old\"}");
        var patch = objectMapper.readTree("{\"name\":null}");

        var result = cut.applyMergePatch(patch, target);

        assertThat(result.has("name")).isFalse();
    }

    @Test
    void applyMergePatch_wraps_malformed_patch_in_validation_exception() throws Exception {
        var target = objectMapper.readTree("{}");
        var patch = objectMapper.readTree("[\"array\",\"not allowed\"]");

        assertThatThrownBy(() -> cut.applyMergePatch(patch, target))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageStartingWith("Invalid patch:");
    }
}
