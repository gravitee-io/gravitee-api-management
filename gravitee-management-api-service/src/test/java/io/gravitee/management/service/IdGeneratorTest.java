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
package io.gravitee.management.service;

import io.gravitee.management.service.impl.IdGeneratorImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class IdGeneratorTest {

    @Test
    public void shouldGenerateCorrectId() {
        IdGenerator generator = new IdGeneratorImpl();

        String id = generator.generate("My  API");
        Assert.assertEquals("my-api", id);
    }

    @Test
    public void shouldGenerateCorrectId2() {
        IdGenerator generator = new IdGeneratorImpl();

        String id = generator.generate("My  API?123");
        Assert.assertEquals("my-api-123", id);
    }

    @Test
    public void shouldGenerateCorrectId3() {
        IdGenerator generator = new IdGeneratorImpl();

        String id = generator.generate("My  API#?=123");
        Assert.assertEquals("my-api-123", id);
    }

    @Test
    public void shouldGenerateCorrectId4() {
        IdGenerator generator = new IdGeneratorImpl();

        String id = generator.generate("My  API#?=");
        Assert.assertEquals("my-api-", id);
    }

    @Test
    public void shouldGenerateCorrectId5() {
        IdGenerator generator = new IdGeneratorImpl();

        String id = generator.generate("éererer");
        Assert.assertEquals("-ererer", id);
    }
}
