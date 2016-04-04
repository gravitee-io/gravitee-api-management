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
package io.gravitee.gateway.handlers.api.validator;

import io.gravitee.gateway.handlers.api.definition.Api;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiValidatorTest {

    @Test(expected = ValidationException.class)
    public void validate_emptyDefinition() {
        Api definition = new Api();

        new ApiValidator().validate(definition);
    }

    @Test(expected = ValidationException.class)
    public void validate_noPathDefinition() {
        Api definition = new Api();
        definition.setName("api-test");

        new ApiValidator().validate(definition);
    }

    @Test(expected = ValidationException.class)
    public void validate_emptyPathDefinition() {
        Api definition = new Api();
        definition.setName("api-test");
        definition.setPaths(new HashMap<>());

        new ApiValidator().validate(definition);
    }
}
