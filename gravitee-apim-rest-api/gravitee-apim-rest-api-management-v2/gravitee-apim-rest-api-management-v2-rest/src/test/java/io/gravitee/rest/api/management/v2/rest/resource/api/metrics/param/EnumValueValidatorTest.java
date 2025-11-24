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
package io.gravitee.rest.api.management.v2.rest.resource.api.metrics.param;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EnumValueValidatorTest {

    EnumValueValidator userTest = new EnumValueValidator();

    @EnumValue(value = TestEnum.class, preserveCase = true)
    String validValue = "VALID";

    @EnumValue(value = TestEnum.class, preserveCase = true)
    String invalidValue = "INVALID";

    @EnumValue(value = TestEnum.class, preserveCase = true)
    String invalidValueWrongCase = "valid";

    @EnumValue(value = TestEnum.class)
    String validValueCI = "also_valid";

    @EnumValue(value = TestEnum.class)
    String invalidValueCI = "invalid";

    @EnumValue(value = TestEnum.class)
    String emptyValue = "";

    @EnumValue(value = TestEnum.class)
    String nullValue = null;

    @ParameterizedTest
    @CsvSource(
        {
            "validValue,true",
            "invalidValue,false",
            "invalidValueWrongCase,false",
            "validValueCI,true",
            "invalidValueCI,false",
            "emptyValue,true",
            "emptyValue,true",
        }
    )
    void should_perform_validation(String fieldName, boolean isValid) throws NoSuchFieldException, IllegalAccessException {
        Field field = EnumValueValidatorTest.class.getDeclaredField(fieldName);
        EnumValue annotation = field.getAnnotation(EnumValue.class);

        userTest.initialize(annotation);
        assertThat(userTest.isValid((String) field.get(this), null)).isEqualTo(isValid);
    }

    enum TestEnum {
        VALID,
        ALSO_VALID,
    }
}
