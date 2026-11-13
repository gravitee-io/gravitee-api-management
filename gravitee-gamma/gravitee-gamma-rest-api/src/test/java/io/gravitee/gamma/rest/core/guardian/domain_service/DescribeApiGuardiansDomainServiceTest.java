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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardians;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DescribeApiGuardiansDomainServiceTest {

    private final CountingGuardianRepository repository = new CountingGuardianRepository();
    private final DescribeApiGuardiansDomainService service = new DescribeApiGuardiansDomainService(repository);

    @Test
    void should_resolve_names_in_one_read_preserving_phase_order_and_missing_guardians() {
        repository.givenGuardian(aGuardian(d -> d.id("second").name("Current second name")).build());
        repository.givenGuardian(aGuardian(d -> d.id("first").name("Current first name")).build());
        repository.givenGuardian(aGuardian(d -> d.id("foreign").environmentId("other-environment").name("Private name")).build());
        var enabled = ApiGuardians.of(
            Flow.builder()
                .request(List.of(GuardianSteps.of("first", "Old name"), GuardianSteps.of("missing", "Missing")))
                .response(List.of(GuardianSteps.of("second", "Old name"), GuardianSteps.of("foreign", "Foreign")))
                .build()
        );

        var described = service.describe(ENVIRONMENT_ID, enabled);

        assertSoftly(softly -> {
            softly
                .assertThat(described)
                .as("current names in execution order, no names from another environment")
                .containsExactly(
                    new ApiGuardian("first", "Current first name", Phase.REQUEST, 1),
                    new ApiGuardian("missing", null, Phase.REQUEST, 2),
                    new ApiGuardian("second", "Current second name", Phase.RESPONSE, 1),
                    new ApiGuardian("foreign", null, Phase.RESPONSE, 2)
                );
            softly.assertThat(repository.singleReads).as("no per-Guardian reads").isZero();
            softly.assertThat(repository.bulkReads).as("one bulk read").isEqualTo(1);
            softly
                .assertThat(repository.requestedIds)
                .as("only enabled ids requested")
                .containsExactlyInAnyOrder("first", "missing", "second", "foreign");
        });
    }

    @Test
    void should_skip_the_repository_when_no_guardian_is_enabled() {
        var described = service.describe(ENVIRONMENT_ID, ApiGuardians.of(null));

        assertSoftly(softly -> {
            softly.assertThat(described).as("no enabled Guardians").isEmpty();
            softly.assertThat(repository.singleReads + repository.bulkReads).as("no unnecessary reads").isZero();
        });
    }

    private static class CountingGuardianRepository extends InMemoryGuardianRepository {

        int singleReads;
        int bulkReads;
        Set<String> requestedIds = Set.of();

        @Override
        public Optional<Guardian> findByEnvironmentId(String environmentId, String guardianId) {
            singleReads++;
            return super.findByEnvironmentId(environmentId, guardianId);
        }

        @Override
        public List<Guardian> findByEnvironmentIdAndIds(String environmentId, Set<String> ids) {
            bulkReads++;
            requestedIds = ids;
            return super.findByEnvironmentIdAndIds(environmentId, ids);
        }
    }
}
