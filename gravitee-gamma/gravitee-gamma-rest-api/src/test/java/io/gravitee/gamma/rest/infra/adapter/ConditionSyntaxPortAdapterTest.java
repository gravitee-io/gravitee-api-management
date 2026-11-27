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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConditionSyntaxPortAdapterTest {

    private final ConditionSyntaxPortAdapter syntax = new ConditionSyntaxPortAdapter();

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{#context.attributes['aihub.team'] == 'team-a'}",
            "{(#context.attributes['aihub.team'] == 'team-a' || #context.attributes['aihub.team'] == 'it''s')}",
            "{(!(#context.attributes['aihub.model'] == 'gpt'))}",
            "{#context.attributes['aihub.groups'] != null && #context.attributes['aihub.groups'].contains('g1')}",
            "{(#context.attributes['aihub.team'] == 'team-a') && !(#context.attributes['aihub.groups'] != null && #context.attributes['aihub.groups'].contains('g1'))}",
        }
    )
    void should_accept_one_expression_the_gateway_evaluates(String condition) {
        assertThat(syntax.errorIn(condition)).as(condition).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(
        strings = { "{#context.attributes['aihub.team'] == }", "{#context.attributes['aihub.team'] == 'team-a'", "{#a == 'x' ||| }" }
    )
    void should_refuse_a_condition_that_does_not_parse(String condition) {
        assertThat(syntax.errorIn(condition)).as(condition).isPresent();
    }

    /**
     * The gateway only recognizes an expression opening with <code>{#</code>, <code>{T</code> or <code>{(</code>; anything else is
     * text, which it cannot read as a boolean, so it reports a warning and skips the step on every call.
     */
    @ParameterizedTest
    @ValueSource(strings = { "{!(#context.attributes['aihub.team'] == 'team-a')}", "team-a", "{#a == 'x'} {#b == 'y'}" })
    void should_refuse_a_condition_the_gateway_reads_as_text_rather_than_as_one_expression(String condition) {
        assertThat(syntax.errorIn(condition))
            .as(condition)
            .hasValueSatisfying(error -> assertThat(error).contains("one expression"));
    }
}
