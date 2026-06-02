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
package io.gravitee.rest.api.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.rest.api.model.descriptor.GraviteeDescriptorEntity;
import io.gravitee.rest.api.service.exceptions.GraviteeDescriptorReadException;
import io.gravitee.rest.api.service.exceptions.GraviteeDescriptorVersionException;
import io.gravitee.rest.api.service.impl.GraviteeDescriptorServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class GraviteeDescriptorService_ReadTest {

    @InjectMocks
    private GraviteeDescriptorServiceImpl service = new GraviteeDescriptorServiceImpl();

    @Test
    public void shouldRead() throws Exception {
        GraviteeDescriptorEntity entity = service.read("{ \"version\": 1}");

        assertNotNull(entity);
        assertEquals(1, entity.getVersion(), "version");
        assertNull(entity.getDocumentation(), "documentation");
    }

    @Test
    public void shouldThrowGraviteeDescriptorReadException() throws Exception {
        assertThrows(GraviteeDescriptorReadException.class, () -> {
            service.read("{ \"unknown\": 1}");

            fail("should throw a GraviteeDescriptorReadException");
        });
    }

    @Test
    public void shouldThrowGraviteeDescriptorVersionException_WrongVersion() throws Exception {
        assertThrows(GraviteeDescriptorVersionException.class, () -> {
            service.read("{ \"version\": 2}");

            fail("should throw a GraviteeDescriptorVersionException");
        });
    }

    @Test
    public void shouldThrowGraviteeDescriptorVersionException_NoVersion() throws Exception {
        assertThrows(GraviteeDescriptorVersionException.class, () -> {
            service.read("{}");

            fail("should throw a GraviteeDescriptorVersionException");
        });
    }
}
