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
package io.gravitee.apim.core.json_patch.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.exception.ValidationDomainException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JsonPatchDomainServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonNode stubResult = objectMapper.createObjectNode().put("patched", true);

    private final JsonPatchDomainService cut = new JsonPatchDomainService((patch, target) -> stubResult, (patch, target) -> stubResult);

    @Test
    void applyMergePatch_delegates_to_merge_patch_service() {
        var target = objectMapper.createObjectNode();
        var patch = objectMapper.createObjectNode().put("name", "new");

        assertThat(cut.applyMergePatch(patch, target)).isSameAs(stubResult);
    }

    @Test
    void applyMergePatch_rejects_non_object_patch() {
        var target = objectMapper.createObjectNode();
        var patch = objectMapper.createArrayNode();

        assertThatThrownBy(() -> cut.applyMergePatch(patch, target))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Invalid patch: a merge patch must be a JSON object");
    }

    @Test
    void applyJsonPatch_delegates_to_json_patch_service() {
        var target = objectMapper.createObjectNode();
        var patch = objectMapper.createArrayNode();

        assertThat(cut.applyJsonPatch(patch, target)).isSameAs(stubResult);
    }

    @Test
    void applyJsonPatch_rejects_non_array_patch() {
        var target = objectMapper.createObjectNode();
        var patch = objectMapper.createObjectNode();

        assertThatThrownBy(() -> cut.applyJsonPatch(patch, target))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Invalid patch: a JSON patch must be an array");
    }
}
