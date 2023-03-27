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
package io.gravitee.gateway.jupiter.flow;

import io.gravitee.definition.model.flow.Flow;
<<<<<<< HEAD:gravitee-apim-gateway/gravitee-apim-gateway-flow/src/main/java/io/gravitee/gateway/jupiter/flow/AbstractFlowResolver.java
import io.gravitee.gateway.jupiter.api.context.GenericExecutionContext;
import io.gravitee.gateway.jupiter.core.condition.ConditionFilter;
=======
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
>>>>>>> d5a816621b (fix(gateway): fix pathParameter for jupiter/v4):gravitee-apim-gateway/gravitee-apim-gateway-flow/src/main/java/io/gravitee/gateway/reactive/flow/AbstractFlowResolver.java
import io.reactivex.rxjava3.core.Flowable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractFlowResolver implements FlowResolver {

    private static final String URL_PATH_SEPARATOR = "/";
    private static final Pattern URL_PATH_SEPARATOR_PATTERN = Pattern.compile(URL_PATH_SEPARATOR);
    private static final String PATH_PARAM_PREFIX = ":";
    private final ConditionFilter<Flow> filter;

    protected AbstractFlowResolver(ConditionFilter<Flow> filter) {
        this.filter = filter;
    }

    public Flowable<Flow> resolve(GenericExecutionContext ctx) {
        return provideFlows(ctx).flatMapMaybe(flow -> filter.filter(ctx, flow));
    }

    protected void addContextRequestPathParameters(GenericExecutionContext context, List<Flow> flows) {
        flows.stream().forEach(flow -> addContextRequestPathParameters(context, flow));
    }

    protected void addContextRequestPathParameters(GenericExecutionContext context, Flow flow) {
        if (flow.getPath() == null || flow.getPath().isEmpty()) {
            return;
        }

        Map<Integer, String> parameters = new HashMap();
        String[] branches = URL_PATH_SEPARATOR_PATTERN.split(flow.getPath());

        for (int position = 0; position < branches.length; position++) {
            final String branch = branches[position];
            if (branch.startsWith(PATH_PARAM_PREFIX)) {
                parameters.put(position, branch.substring(PATH_PARAM_PREFIX.length()));
            }
        }

        if (parameters.isEmpty()) {
            return;
        }

        int count = 0;
        int off = 0;
        int next;

        final String pathInfo = context.request().pathInfo();
        final Iterator<Integer> iterator = parameters.keySet().iterator();

        Integer currentPosition = iterator.next();
        while ((next = pathInfo.indexOf(URL_PATH_SEPARATOR, off)) != -1) {
            if (count == currentPosition) {
                context.request().pathParameters().add(parameters.get(currentPosition), pathInfo.substring(off, next));
                if (iterator.hasNext()) {
                    currentPosition = iterator.next();
                }
            }
            off = next + 1;
            count++;
        }

        context.request().pathParameters().add(parameters.get(currentPosition), pathInfo.substring(off));
    }
}
