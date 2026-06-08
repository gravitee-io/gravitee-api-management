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
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JsonPatchServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonPatchServiceImpl cut = new JsonPatchServiceImpl();

    @Test
    void runtime_exception_from_library_propagates_without_wrapping_as_validation_exception() throws Exception {
        var patch = objectMapper.readTree("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"new\"}]");

        assertThatThrownBy(() -> cut.applyJsonPatch(patch, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void out_of_bounds_array_index_add_is_wrapped_as_validation_exception() throws Exception {
        var target = objectMapper.readTree("{\"labels\":[\"a\"]}");
        var patch = objectMapper.readTree("[{\"op\":\"add\",\"path\":\"/labels/5\",\"value\":\"x\"}]");

        assertThatThrownBy(() -> cut.applyJsonPatch(patch, target)).isInstanceOf(ValidationDomainException.class);

        assertThat(target.get("labels").size()).isEqualTo(1);
    }
}
