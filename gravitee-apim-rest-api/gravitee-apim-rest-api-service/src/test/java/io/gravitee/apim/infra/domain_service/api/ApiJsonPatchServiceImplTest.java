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
package io.gravitee.apim.infra.domain_service.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.exception.ValidationDomainException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiJsonPatchServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiJsonPatchServiceImpl cut = new ApiJsonPatchServiceImpl();

    @Test
    void runtime_exception_from_library_propagates_without_wrapping_as_validation_exception() throws Exception {
        var patch = objectMapper.readTree("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"new\"}]");

        assertThatThrownBy(() -> cut.applyJsonPatch(patch, null)).isInstanceOf(NullPointerException.class);
    }
}
