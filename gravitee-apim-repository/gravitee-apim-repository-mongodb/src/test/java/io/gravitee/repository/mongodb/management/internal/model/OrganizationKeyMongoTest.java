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
package io.gravitee.repository.mongodb.management.internal.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrganizationKeyMongoTest {

    @Test
    void should_keep_the_wrapped_key_out_of_to_string() {
        var key = new OrganizationKeyMongo();
        key.setId("key-1");
        key.setOrganizationId("org-1");
        key.setWrappedKey("wrapped-1");

        assertThat(key.toString()).contains("key-1", "org-1").doesNotContain("wrapped-1");
    }
}
