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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class EmailValidatorTest {

    private static final String[] VALID_EMAILS = {
        "email@gravitee.io",
        "firstname.lastname@gravitee.io",
        "email@subdomain.gravitee.io",
        "firstname+lastname@gravitee.io",
        "firstname.lastname+1-test@gravitee.io",
        "1234567890@gravitee.io",
        "email@gravitee-io.com",
        "_______@gravitee.io",
        "firstname-lastname@gravitee.io",
        "firstname-lastname@gravitee.io",
    };

    private static final String[] INVALID_EMAILS = {
        "email",
        "#@%^%#$@#$@#.com",
        "@gravitee.io",
        "Joe Smith <email@gravitee.io>",
        "email@gravitee@gravitee.io",
        ".email@gravitee.io",
        "email.@gravitee.io",
        "email..email@gravitee.io",
        "email@gravitee",
        "email@gravitee",
        "email@gravitee..io",
    };

    @Test
    public void validate() {
        for (String email : VALID_EMAILS) {
            assertTrue(email + " should be valid", EmailValidator.isValid(email));
        }
    }

    @Test
    public void validate_notValid() {
        for (String email : INVALID_EMAILS) {
            assertFalse(email + " should be invalid", EmailValidator.isValid(email));
        }
    }
}
