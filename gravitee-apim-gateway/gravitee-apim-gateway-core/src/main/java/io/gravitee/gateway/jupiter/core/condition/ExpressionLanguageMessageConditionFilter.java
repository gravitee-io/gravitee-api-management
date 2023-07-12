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
package io.gravitee.gateway.jupiter.core.condition;

import io.gravitee.definition.model.MessageConditionSupplier;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.el.spel.function.xml.DocumentBuilderFactoryUtils;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.reactivex.rxjava3.core.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ConditionFilter} base on an EL expression.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExpressionLanguageMessageConditionFilter<T extends MessageConditionSupplier> implements MessageConditionFilter<T> {

    private static final Logger log = LoggerFactory.getLogger(DocumentBuilderFactoryUtils.class);

    @Override
    public Maybe<T> filter(MessageExecutionContext ctx, T elt, Message message) {
        final String condition = elt.getMessageCondition();

        if (condition == null || condition.isEmpty()) {
            return Maybe.just(elt);
        }

        return ctx
            .getTemplateEngine(message)
            .eval(condition, Boolean.class)
            .filter(Boolean::booleanValue)
            .map(aBoolean -> elt)
            .onErrorComplete(ExpressionEvaluationException.class::isInstance)
            .doOnError(throwable -> log.warn("Error parsing condition {}", condition, throwable));
    }
}
