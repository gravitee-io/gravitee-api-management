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
package io.gravitee.definition.model.v4.agent.evaluation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Which recorded runs an evaluation scores — a <em>selection</em>, not a list.
 *
 * <p>Runs are chosen from what was recorded rather than authored as test cases, so an evaluation is always about
 * traffic that really happened. The consequence is worth stating plainly: there is no expected answer to compare
 * against, because nobody wrote one. A check here asks "does this answer cite a source", never "is this the answer we
 * agreed on".</p>
 *
 * <p>The selection is re-resolved on every evaluation, so the same dataset names a different set of runs each time it
 * runs. That is the point for monitoring and a trap for comparison: two evaluations of {@code since: 24h} a day apart
 * share no runs, and their scores are two samples of behaviour rather than a before and after.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dataset {

    /** Any run, whatever it ended as. The default. */
    public static final String OUTCOME_ANY = "any";
    /** Only runs that failed. */
    public static final String OUTCOME_ERROR = "error";
    /** Only runs that answered. */
    public static final String OUTCOME_SUCCESS = "success";

    /** The id of the agent whose runs are scored. Required — an evaluation is always about one agent. */
    private String agent;

    /**
     * How far back to look, as a duration: {@code 30m}, {@code 24h}, {@code 7d}. Absent ⇒ 24 hours.
     *
     * <p>A window rather than an absolute range because the useful question is nearly always "since last time", and a
     * definition carrying fixed dates goes stale the day after it is written.</p>
     */
    private String since;

    /** {@code any} | {@code error} | {@code success}. Absent ⇒ {@link #OUTCOME_ANY}. */
    private String outcome;

    /**
     * The most runs to score in one evaluation. Absent ⇒ 100.
     *
     * <p>A ceiling, not a target, and deliberately low: every run selected here is multiplied by the number of
     * evaluators attached, and a judge among them means a model call each. An unbounded selection over a busy agent is
     * an unbounded bill.</p>
     */
    private Integer limit;
}
