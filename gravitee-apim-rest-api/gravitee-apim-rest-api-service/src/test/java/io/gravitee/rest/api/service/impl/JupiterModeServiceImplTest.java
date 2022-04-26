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

import static io.gravitee.rest.api.service.JupiterModeService.DefaultMode.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.ExecutionMode;
import org.junit.Test;

/**
 * @author Guillaume Guillaume (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JupiterModeServiceImplTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Test
    public void shouldSetDefault() {
        JupiterModeServiceImpl jupiterModeService = new JupiterModeServiceImpl(false, ALWAYS.getLabel());
        assertFalse(jupiterModeService.isEnabled());
        assertEquals(ALWAYS, jupiterModeService.defaultMode());
    }

    @Test
    public void shouldReturnJupiterWithoutApiDefWhenEnabledAndDefaultAlways() {
        JupiterModeServiceImpl jupiterModeService = new JupiterModeServiceImpl(true, ALWAYS.getLabel());
        assertTrue(jupiterModeService.isEnabled());
        assertEquals(ALWAYS, jupiterModeService.defaultMode());
        assertEquals(ExecutionMode.JUPITER, jupiterModeService.getExecutionModeFor(null));
    }

    @Test
    public void shouldReturnJupiterWithoutApiDefWhenEnabledAndDefaultCreationOnly() {
        JupiterModeServiceImpl jupiterModeService = new JupiterModeServiceImpl(true, CREATION_ONLY.getLabel());
        assertTrue(jupiterModeService.isEnabled());
        assertEquals(CREATION_ONLY, jupiterModeService.defaultMode());
        assertEquals(ExecutionMode.JUPITER, jupiterModeService.getExecutionModeFor(null));
    }

    @Test
    public void shouldReturnV3WithoutApiDefWhenEnabledAndDefaultNever() {
        JupiterModeServiceImpl jupiterModeService = new JupiterModeServiceImpl(true, NEVER.getLabel());
        assertTrue(jupiterModeService.isEnabled());
        assertEquals(NEVER, jupiterModeService.defaultMode());
        assertEquals(ExecutionMode.V3, jupiterModeService.getExecutionModeFor(null));
    }

    @Test
    public void shouldReturnV3WithoutApiDefWhenDisabledAndDefaultAlways() {
        JupiterModeServiceImpl jupiterModeService = new JupiterModeServiceImpl(false, ALWAYS.getLabel());
        assertFalse(jupiterModeService.isEnabled());
        assertEquals(ALWAYS, jupiterModeService.defaultMode());
        assertEquals(ExecutionMode.V3, jupiterModeService.getExecutionModeFor(null));
    }

    @Test
    public void shouldReturnV3WithoutApiDefWhenDisabledAndDefaultCreationOnly() {
        JupiterModeServiceImpl jupiterModeService = new JupiterModeServiceImpl(false, CREATION_ONLY.getLabel());
        assertFalse(jupiterModeService.isEnabled());
        assertEquals(CREATION_ONLY, jupiterModeService.defaultMode());
        assertEquals(ExecutionMode.V3, jupiterModeService.getExecutionModeFor(null));
    }

    @Test
    public void shouldReturnJupiterWithApiDefWhenEnabledAndDefaultAlways() {
        JupiterModeServiceImpl jupiterModeService = new JupiterModeServiceImpl(true, ALWAYS.getLabel());
        assertTrue(jupiterModeService.isEnabled());
        assertEquals(ALWAYS, jupiterModeService.defaultMode());
        assertEquals(ExecutionMode.JUPITER, jupiterModeService.getExecutionModeFor(objectMapper.createObjectNode()));
    }

    @Test
    public void shouldReturnV3WithApiDefWhenEnabledAndDefaultCreationOnly() {
        JupiterModeServiceImpl jupiterModeService = new JupiterModeServiceImpl(true, CREATION_ONLY.getLabel());
        assertTrue(jupiterModeService.isEnabled());
        assertEquals(CREATION_ONLY, jupiterModeService.defaultMode());
        assertEquals(ExecutionMode.V3, jupiterModeService.getExecutionModeFor(objectMapper.createObjectNode()));
    }

    @Test
    public void shouldReturnV3WithApiDefWhenEnabledAndDefaultNever() {
        JupiterModeServiceImpl jupiterModeService = new JupiterModeServiceImpl(true, NEVER.getLabel());
        assertTrue(jupiterModeService.isEnabled());
        assertEquals(NEVER, jupiterModeService.defaultMode());
        assertEquals(ExecutionMode.V3, jupiterModeService.getExecutionModeFor(objectMapper.createObjectNode()));
    }

    @Test
    public void shouldReturnV3WithApiDefWhenDisabledAndDefaultAlways() {
        JupiterModeServiceImpl jupiterModeService = new JupiterModeServiceImpl(false, ALWAYS.getLabel());
        assertFalse(jupiterModeService.isEnabled());
        assertEquals(ALWAYS, jupiterModeService.defaultMode());
        assertEquals(ExecutionMode.V3, jupiterModeService.getExecutionModeFor(objectMapper.createObjectNode()));
    }

    @Test
    public void shouldReturnV3WithApiDefWhenDisabledAndDefaultCreationOnly() {
        JupiterModeServiceImpl jupiterModeService = new JupiterModeServiceImpl(false, CREATION_ONLY.getLabel());
        assertFalse(jupiterModeService.isEnabled());
        assertEquals(CREATION_ONLY, jupiterModeService.defaultMode());
        assertEquals(ExecutionMode.V3, jupiterModeService.getExecutionModeFor(objectMapper.createObjectNode()));
    }
}
