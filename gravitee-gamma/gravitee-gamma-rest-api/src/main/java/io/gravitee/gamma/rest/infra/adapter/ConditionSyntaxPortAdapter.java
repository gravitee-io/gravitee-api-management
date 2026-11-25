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

import io.gravitee.el.spel.SpelExpressionParser;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.ConditionSyntaxPort;
import java.util.Optional;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpression;

/**
 * Parses a condition with the parser of the gateway's expression language, the one its template engine runs on a step
 * condition before evaluating it — so what this accepts, the gateway parses.
 *
 * <p>Parsing alone accepts more than the gateway can evaluate: that parser only recognizes an expression opening with
 * <code>{#</code>, <code>{T</code> or <code>{(</code>, and reads anything else as text — <code>{!…}</code> included. A step condition read as
 * text, or holding text around an expression, does not evaluate to a boolean, and the gateway then skips the step on
 * every call. A condition is therefore accepted only when it parses as one expression and nothing else.
 *
 * @author GraviteeSource Team
 */
public class ConditionSyntaxPortAdapter implements ConditionSyntaxPort {

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Override
    public Optional<String> errorIn(String condition) {
        try {
            if (parser.parseExpression(condition) instanceof SpelExpression) {
                return Optional.empty();
            }
        } catch (ParseException e) {
            return Optional.of(e.getMessage());
        }
        return Optional.of(
            "it is not one expression: the gateway reads a condition as one {#…}, {(…)} or {T(…)…} block with nothing " +
                "around it, and anything else as text — wrap a negation as {(!…)}, not {!…}."
        );
    }
}
