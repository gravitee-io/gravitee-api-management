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
package io.gravitee.definition.jackson.services;

import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServicesDeserializerTest extends AbstractTest {

    @Test
    public void definition_withServices() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/api-withservices.json", Api.class);

        Assertions.assertNotNull(api);
        Assertions.assertFalse(api.getServices().isEmpty());
        Assertions.assertEquals(2, api.getServices().getAll().size());
    }
}
