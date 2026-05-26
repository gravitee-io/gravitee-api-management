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
package io.gravitee.apim.infra.domain_service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.user.model.RawPassword;
import io.gravitee.rest.api.service.PasswordValidator;
import io.gravitee.rest.api.service.exceptions.PasswordFormatInvalidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserPasswordServiceImplTest {

    PasswordValidator passwordValidator = mock(PasswordValidator.class);
    UserPasswordServiceImpl service = new UserPasswordServiceImpl(passwordValidator);

    @Nested
    class Validate {

        @Test
        void should_not_throw_when_password_is_valid() {
            when(passwordValidator.validate("S3cr3t!")).thenReturn(true);

            var throwable = catchThrowable(() -> service.validate(new RawPassword("S3cr3t!")));

            assertThat(throwable).isNull();
        }

        @Test
        void should_throw_when_password_is_invalid() {
            when(passwordValidator.validate("weak")).thenReturn(false);

            var throwable = catchThrowable(() -> service.validate(new RawPassword("weak")));

            assertThat(throwable).isInstanceOf(PasswordFormatInvalidException.class);
        }
    }

    @Nested
    class Encode {

        @Test
        void should_return_bcrypt_encoded_password() {
            var result = service.encode(new RawPassword("S3cr3t!"));

            assertThat(result.value()).startsWith("$2a$");
        }

        @Test
        void should_return_different_hashes_for_same_input() {
            var first = service.encode(new RawPassword("S3cr3t!"));
            var second = service.encode(new RawPassword("S3cr3t!"));

            assertThat(first.value()).isNotEqualTo(second.value());
        }
    }
}
