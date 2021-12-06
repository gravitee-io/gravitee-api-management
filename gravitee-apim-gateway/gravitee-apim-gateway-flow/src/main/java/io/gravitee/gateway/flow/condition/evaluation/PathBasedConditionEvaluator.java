/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.flow.condition.evaluation;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * This {@link ConditionEvaluator} evaluates to true if the path of the request is matching the
 * path declared within the {@link Flow} depending on the {@link Operator}
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathBasedConditionEvaluator implements ConditionEvaluator<Flow> {

    private static final char OPTIONAL_TRAILING_SEPARATOR = '?';
    private static final String PATH_SEPARATOR = "/";
    private static final String PATH_PARAM_PREFIX = ":";
    private static final String PATH_PARAM_REGEX = "[a-zA-Z0-9\\-._~%!$&'()* +,;=:@/]+";

    private final Map<String, Pattern> cache = new ConcurrentHashMap<>();

    @Override
    public boolean evaluate(Flow flow, ExecutionContext context) {
        Pattern pattern = cache.computeIfAbsent(flow.getPath(), this::transform);

        return (flow.getOperator() == Operator.EQUALS)
            ? pattern.matcher(context.request().pathInfo()).matches()
            : pattern.matcher(context.request().pathInfo()).lookingAt();
    }

    private Pattern transform(String path) {
        String[] branches = path.split(PATH_SEPARATOR);
        StringBuilder buffer = new StringBuilder(PATH_SEPARATOR);

        for (final String branch : branches) {
            if (!branch.isEmpty()) {
                if (branch.startsWith(PATH_PARAM_PREFIX)) {
                    buffer.append(PATH_PARAM_REGEX);
                } else {
                    buffer.append(branch);
                }

                buffer.append(PATH_SEPARATOR);
            }
        }

        // Last path separator is not required to match
        buffer.append(OPTIONAL_TRAILING_SEPARATOR);

        return Pattern.compile(buffer.toString());
    }
}
