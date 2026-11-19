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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.engine.api.query.EndUser;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EndUserClauseTest {

    @Test
    void should_match_named_users_as_terms() {
        var clause = EndUserClause.of("user", List.of("alice", "bob"));

        assertThat(clause).isEqualTo(new JsonObject("{\"terms\":{\"user\":[\"alice\",\"bob\"]}}"));
    }

    @Test
    void should_match_calls_without_a_user_for_the_none_key() {
        var clause = EndUserClause.of("user", List.of(EndUser.NONE));

        assertThat(clause).isEqualTo(new JsonObject("{\"bool\":{\"must_not\":{\"exists\":{\"field\":\"user\"}}}}"));
    }

    @Test
    void should_match_named_users_or_calls_without_one_when_both_are_asked() {
        var clause = EndUserClause.of("user", List.of("alice", EndUser.NONE));

        assertThat(clause).isEqualTo(
            new JsonObject(
                "{\"bool\":{\"should\":[{\"terms\":{\"user\":[\"alice\"]}},{\"bool\":{\"must_not\":{\"exists\":{\"field\":\"user\"}}}}],\"minimum_should_match\":1}}"
            )
        );
    }
}
