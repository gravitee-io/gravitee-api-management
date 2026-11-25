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
package fixtures.core.model;

import io.gravitee.apim.core.log.model.GuardianDecision;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** A Guardian decision shaped like the ones the gateway reports, for the in-memory query service. */
public final class GuardianDecisionFixtures {

    public static final String ORGANIZATION_ID = "DEFAULT";
    public static final String ENVIRONMENT_ID = "DEFAULT";
    public static final long TIMESTAMP = Instant.parse("2026-01-01T12:00:00Z").toEpochMilli();

    private GuardianDecisionFixtures() {}

    /** An allowed request phase run of {@code guardian-id} on {@code api-id}. */
    public static GuardianDecision.GuardianDecisionBuilder aGuardianDecision() {
        return GuardianDecision.builder()
            .id("decision-id")
            .timestamp(TIMESTAMP)
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .gatewayId("gateway-id")
            .apiId("api-id")
            .guardianId("guardian-id")
            .guardianRevision("1")
            .checkpoint("request.llm-proxy")
            .caller("Guardian")
            .requestId("request-id")
            .outcome("ALLOW")
            .enforced("ALLOW")
            .status("success")
            .reasons(List.of("guardrail: CONTINUE — llm-rule did not match"))
            .matchedRules(List.of())
            .durationNanos(1_000_000L)
            .subjectType("agent")
            .subjectId("-1");
    }

    /** A request phase denial: one guardrail blocked the call where it matched. */
    public static GuardianDecision.GuardianDecisionBuilder aDeniedGuardianDecision() {
        return aGuardianDecision()
            .outcome("DENY")
            .enforced("DENY")
            .verdict("GUARDRAIL_BLOCKED")
            .reasons(List.of("guardrail: BLOCK — Blocked by guardrail; llm-rule matched: messages[0].user"))
            .matchedRules(
                List.of(
                    new GuardianDecision.MatchedRule(
                        null,
                        null,
                        null,
                        "DENY",
                        Map.of("guardrail", "guardrail", "location", "messages[0].user")
                    )
                )
            );
    }
}
