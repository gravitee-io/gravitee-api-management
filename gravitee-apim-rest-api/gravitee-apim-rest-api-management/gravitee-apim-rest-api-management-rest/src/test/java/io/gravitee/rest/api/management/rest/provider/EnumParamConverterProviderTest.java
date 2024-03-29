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
package io.gravitee.rest.api.management.rest.provider;

import static org.junit.Assert.*;

import jakarta.ws.rs.ext.ParamConverter;
import org.junit.Test;

public class EnumParamConverterProviderTest {

    enum TestEnum {
        VALUE_1,
        VALUE_2,
    }

    ParamConverter<TestEnum> provider = new EnumParamConverterProvider().getConverter(TestEnum.class, null, null);

    @Test
    public void shouldGetTestEnumConverter() {
        assertNotNull(new EnumParamConverterProvider().getConverter(TestEnum.class, null, null));
    }

    @Test
    public void shouldNotGetConverterForNonEnumType() {
        assertNull(new EnumParamConverterProvider().getConverter(String.class, null, null));
    }

    @Test
    public void shouldParseSameCaseValue() {
        assertEquals(TestEnum.VALUE_1, provider.fromString("VALUE_1"));
    }

    @Test
    public void shouldParseDifferentCaseValue() {
        assertEquals(TestEnum.VALUE_1, provider.fromString("vAlUe_1"));
    }

    @Test
    public void shouldNotFindEnum() {
        assertNull(provider.fromString("unknown_value"));
    }
}
