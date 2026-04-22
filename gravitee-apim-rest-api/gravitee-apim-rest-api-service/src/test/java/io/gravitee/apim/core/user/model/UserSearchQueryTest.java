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
package io.gravitee.apim.core.user.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserSearchQueryTest {

    @Test
    void should_default_query_when_query_is_null() {
        assertThat(new UserSearchQuery(null).query()).isEqualTo(UserSearchQuery.DEFAULT_QUERY);
    }

    @Test
    void should_default_query_when_query_is_blank() {
        assertThat(new UserSearchQuery("   ").query()).isEqualTo(UserSearchQuery.DEFAULT_QUERY);
    }

    @Test
    void should_keep_query_when_query_is_not_blank() {
        assertThat(new UserSearchQuery("john").query()).isEqualTo("john");
    }
}
