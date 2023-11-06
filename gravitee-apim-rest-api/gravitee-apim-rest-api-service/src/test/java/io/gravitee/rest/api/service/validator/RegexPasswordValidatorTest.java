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
package io.gravitee.rest.api.service.validator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class RegexPasswordValidatorTest {

    @ParameterizedTest
    @MethodSource("providerValidatePassword")
    void validatePassword(String password, boolean expected) {
        RegexPasswordValidator validator = new RegexPasswordValidator(
            "^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[!~<>.,;:_=?/*+\\-#\\\"'&§`£€%°()|\\[\\]$^@])(?!.*(.)\\1{2,}).{12,128}$"
        );
        assertEquals(expected, validator.validate(password));
    }

    private static Stream<Arguments> providerValidatePassword() {
        return Stream.of(
            Arguments.of("a1!atjubclzf", false),
            Arguments.of("A1!ABVREFAGD", false),
            Arguments.of("Aa!AHYaeffSF", false),
            Arguments.of("AaBbCcDd1324", false),
            Arguments.of("Aa1!", false),
            Arguments.of("Password12!", false),
            Arguments.of("Passsword123!", false),
            Arguments.of("Password123!!!", false),
            Arguments.of(
                "MA48*xP:344MA48*xP:344MA48*xP:344MA48*xP:344MA48*xP:344MA48*xP:344MA48*xP:344MA48*xP:344MA48*xP:344MA48*xP:344MA48*xP:344MA48*xPz:",
                false
            ),
            Arguments.of("Password1231!", true),
            Arguments.of("Password123!2£1", true),
            Arguments.of("MA48*xP:344d", true),
            Arguments.of("Ab0!~<>,;:_-=?*+#.\"'&§`€%°()|[]$^@", true)
        );
    }
}
