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
package io.gravitee.repository.log.v4.model.decision;

import java.util.Objects;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * What a decision search narrows on.
 *
 * <p>{@link #decisionPointType} is the one required predicate, and it is a parameter rather than a
 * constant of the caller: every decision point writes to the same data stream, so a search that does not
 * name a family reads guardian verdicts, human approvals and external approvals as one list. Today's
 * caller asks for {@code guardian}; tomorrow's asks for {@code human-approval} through this same class.
 *
 * <p>The api is <em>not</em> required: the first consumer lists a whole environment's decisions, and
 * making the api a mandatory scope would rule that view out. Callers that read on behalf of a user pass
 * the apis that user may see. The organization and the environment are not part of this query — they
 * come from the query context, and the repository turns them into predicates of their own, because the
 * data stream is shared by every environment.
 *
 * <p>The {@code excluded*} fields negate their twin, and they are read differently from {@link #apiIds}:
 * {@code null} and an empty set say the same thing there — nothing is ruled out. A screen that lets the
 * user tick values off builds its exclusion list from the ticks, and an untouched list must leave the page
 * as it was rather than empty it. Only the four dimensions a caller can negate carry a twin; widening the
 * set is a one-line addition here and in the adapter.
 *
 * <p>A value that is at once included and excluded is excluded: the repository hands both clauses to the
 * index, which applies the negation last.
 *
 * @author GraviteeSource Team
 */
@Data
@Builder
public class DecisionLogQuery {

    public static final int MAX_SIZE = 1_000;

    /** Which kind of decision point to read. Required — see the class javadoc. */
    private String decisionPointType;

    private Set<String> apiIds;
    private Set<String> excludedApiIds;
    private Set<String> applicationIds;
    private Set<String> excludedApplicationIds;
    private Set<String> planIds;
    private Long from;
    private Long to;
    private Set<String> decisionPointIds;
    private Set<String> excludedDecisionPointIds;
    private Set<String> checkpoints;
    private Set<String> callers;
    private Set<String> outcomes;
    private Set<String> excludedOutcomes;
    private Set<String> enforcements;
    private Set<String> verdicts;
    private Set<String> statuses;
    private Set<String> subjectIds;
    private Set<String> actorIds;
    private Set<String> actions;
    private Set<String> resourceIds;
    private Set<String> caseIds;
    private Set<String> requestIds;
    private Set<String> traceIds;
    private String reasonContains;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private int page = 1;

    public void validate() {
        Objects.requireNonNull(decisionPointType, "decisionPointType");
        if (decisionPointType.isBlank()) {
            throw new IllegalArgumentException("decisionPointType must not be blank");
        }
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1, was " + page);
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1, was " + size);
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be <= " + MAX_SIZE + ", was " + size);
        }
        if (from != null && to != null && from > to) {
            throw new IllegalArgumentException("from must be <= to, was from=" + from + " to=" + to);
        }
    }
}
