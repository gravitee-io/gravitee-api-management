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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.descriptor.GraviteeDescriptorEntity;
import io.gravitee.rest.api.service.exceptions.GraviteeDescriptorReadException;
import io.gravitee.rest.api.service.exceptions.GraviteeDescriptorVersionException;
import io.gravitee.rest.api.service.impl.GraviteeDescriptorServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class GraviteeDescriptorService_ReadTest {

    @InjectMocks
    private GraviteeDescriptorServiceImpl service = new GraviteeDescriptorServiceImpl();

    @Test
    public void shouldRead() throws Exception {
        GraviteeDescriptorEntity entity = service.read("{ \"version\": 1}");

        assertNotNull(entity);
        assertEquals("version", 1, entity.getVersion());
        assertNull("documentation", entity.getDocumentation());
    }

    @Test(expected = GraviteeDescriptorReadException.class)
    public void shouldThrowGraviteeDescriptorReadException() throws Exception {
        service.read("{ \"unknown\": 1}");

        fail("should throw a GraviteeDescriptorReadException");
    }

    @Test(expected = GraviteeDescriptorVersionException.class)
    public void shouldThrowGraviteeDescriptorVersionException_WrongVersion() throws Exception {
        service.read("{ \"version\": 2}");

        fail("should throw a GraviteeDescriptorVersionException");
    }

    @Test(expected = GraviteeDescriptorVersionException.class)
    public void shouldThrowGraviteeDescriptorVersionException_NoVersion() throws Exception {
        service.read("{}");

        fail("should throw a GraviteeDescriptorVersionException");
    }

}
