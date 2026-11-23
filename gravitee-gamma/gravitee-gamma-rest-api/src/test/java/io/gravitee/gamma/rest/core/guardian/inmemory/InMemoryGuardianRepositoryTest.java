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
package io.gravitee.gamma.rest.core.guardian.inmemory;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianSearchQuery;
import java.time.Instant;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The order the double answers, pinned against the one both real backends answer — it is published in
 * this module's test-jar, so a consumer's test reads it as the repository contract.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class InMemoryGuardianRepositoryTest {

    private final InMemoryGuardianRepository repository = new InMemoryGuardianRepository();

    @Test
    void should_order_on_the_creation_date_when_the_query_carries_no_sort() {
        givenGuardian("last", Instant.parse("2026-03-01T00:00:00Z"));
        givenGuardian("first", Instant.parse("2026-01-01T00:00:00Z"));
        givenGuardian("middle", Instant.parse("2026-02-01T00:00:00Z"));

        var page = repository.search(new GuardianSearchQuery(ENVIRONMENT_ID, null, 1, 10, null));

        assertThat(page.getContent())
            .as("no sortBy: MongoGuardianRepository and JdbcGuardianRepository both default to created_at ascending")
            .extracting(Guardian::getId)
            .containsExactly("first", "middle", "last");
    }

    @Test
    void should_break_ties_on_the_id_whatever_the_order_asked_for() {
        var sameDate = Instant.parse("2026-01-01T00:00:00Z");
        givenGuardian("c", sameDate);
        givenGuardian("a", sameDate);
        givenGuardian("b", sameDate);

        var ascending = repository.search(new GuardianSearchQuery(ENVIRONMENT_ID, null, 1, 10, "createdAt"));
        var descending = repository.search(new GuardianSearchQuery(ENVIRONMENT_ID, null, 1, 10, "-createdAt"));

        SoftAssertions.assertSoftly(soft -> {
            soft
                .assertThat(ascending.getContent())
                .as("rows sharing the sorted value are ordered by id, not by insertion")
                .extracting(Guardian::getId)
                .containsExactly("a", "b", "c");
            soft
                .assertThat(descending.getContent())
                .as("the tie-breaker stays ascending on a descending sort, as the backends' trailing 'id ASC' does")
                .extracting(Guardian::getId)
                .containsExactly("a", "b", "c");
        });
    }

    @Test
    void should_not_repeat_nor_skip_a_guardian_across_two_pages_of_tied_rows() {
        var sameDate = Instant.parse("2026-01-01T00:00:00Z");
        givenGuardian("c", sameDate);
        givenGuardian("a", sameDate);
        givenGuardian("b", sameDate);
        givenGuardian("d", sameDate);

        var first = repository.search(new GuardianSearchQuery(ENVIRONMENT_ID, null, 1, 2, null));
        var second = repository.search(new GuardianSearchQuery(ENVIRONMENT_ID, null, 2, 2, null));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(first.getContent()).extracting(Guardian::getId).as("first page").containsExactly("a", "b");
            soft.assertThat(second.getContent()).extracting(Guardian::getId).as("second page").containsExactly("c", "d");
        });
    }

    private void givenGuardian(String id, Instant createdAt) {
        repository.givenGuardian(
            aGuardian(d -> d.id(id))
                .createdAt(createdAt)
                .build()
        );
    }
}
