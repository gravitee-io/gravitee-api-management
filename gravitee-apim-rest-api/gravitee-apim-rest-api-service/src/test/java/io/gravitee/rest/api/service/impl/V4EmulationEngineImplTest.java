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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.service.V4EmulationEngineService.DefaultMode.CREATION_ONLY;
import static io.gravitee.rest.api.service.V4EmulationEngineService.DefaultMode.NO;
import static io.gravitee.rest.api.service.V4EmulationEngineService.DefaultMode.YES;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.ExecutionMode;
import org.junit.Test;

/**
 * @author Guillaume Guillaume (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class V4EmulationEngineImplTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Test
    public void should_set_default() {
        V4EmulationEngineServiceImpl v4EmulationEngineService = new V4EmulationEngineServiceImpl(YES.getLabel());
        assertEquals(YES, v4EmulationEngineService.defaultMode());
    }

    @Test
    public void should_return_v4_emulation_engine_without_api_def_when_default_yes() {
        V4EmulationEngineServiceImpl v4EmulationEngineService = new V4EmulationEngineServiceImpl(YES.getLabel());
        assertEquals(YES, v4EmulationEngineService.defaultMode());
        assertEquals(ExecutionMode.V4_EMULATION_ENGINE, v4EmulationEngineService.getExecutionModeFor(null));
    }

    @Test
    public void should_return_v4_emulation_engine_without_api_def_when_default_creation_only() {
        V4EmulationEngineServiceImpl v4EmulationEngineService = new V4EmulationEngineServiceImpl(CREATION_ONLY.getLabel());
        assertEquals(CREATION_ONLY, v4EmulationEngineService.defaultMode());
        assertEquals(ExecutionMode.V4_EMULATION_ENGINE, v4EmulationEngineService.getExecutionModeFor(null));
    }

    @Test
    public void should_return_v3_without_api_def_when_default_no() {
        V4EmulationEngineServiceImpl v4EmulationEngineService = new V4EmulationEngineServiceImpl(NO.getLabel());
        assertEquals(NO, v4EmulationEngineService.defaultMode());
        assertEquals(ExecutionMode.V3, v4EmulationEngineService.getExecutionModeFor(null));
    }

    @Test
    public void should_return_v4_emulation_engine_with_api_def_when_default_yes() {
        V4EmulationEngineServiceImpl v4EmulationEngineService = new V4EmulationEngineServiceImpl(YES.getLabel());
        assertEquals(YES, v4EmulationEngineService.defaultMode());
        assertEquals(ExecutionMode.V4_EMULATION_ENGINE, v4EmulationEngineService.getExecutionModeFor(objectMapper.createObjectNode()));
    }

    @Test
    public void should_return_v3_with_api_def_when_default_creation_only() {
        V4EmulationEngineServiceImpl v4EmulationEngineService = new V4EmulationEngineServiceImpl(CREATION_ONLY.getLabel());
        assertEquals(CREATION_ONLY, v4EmulationEngineService.defaultMode());
        assertEquals(ExecutionMode.V3, v4EmulationEngineService.getExecutionModeFor(objectMapper.createObjectNode()));
    }

    @Test
    public void should_return_v3_with_api_def_when_default_no() {
        V4EmulationEngineServiceImpl v4EmulationEngineService = new V4EmulationEngineServiceImpl(NO.getLabel());
        assertEquals(NO, v4EmulationEngineService.defaultMode());
        assertEquals(ExecutionMode.V3, v4EmulationEngineService.getExecutionModeFor(objectMapper.createObjectNode()));
    }
}
