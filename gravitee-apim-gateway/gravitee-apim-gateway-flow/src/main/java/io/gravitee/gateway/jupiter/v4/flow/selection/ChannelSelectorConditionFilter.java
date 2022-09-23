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
package io.gravitee.gateway.jupiter.v4.flow.selection;

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;

import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.gateway.flow.condition.evaluation.PathPatterns;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.GenericExecutionContext;
import io.gravitee.gateway.jupiter.core.condition.ConditionFilter;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * This {@link ConditionFilter} evaluates to true if the request is matching the
 * channel selector declared within the {@link Flow}.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ChannelSelectorConditionFilter implements ConditionFilter<Flow> {

    private final PathPatterns pathPatterns = new PathPatterns();

    @Override
    public Maybe<Flow> filter(final GenericExecutionContext ctx, final Flow flow) {
        return Maybe.fromCallable(
            () -> {
                Optional<Selector> selectorOptional = flow.selectorByType(SelectorType.CHANNEL);
                if (selectorOptional.isPresent()) {
                    ChannelSelector channelSelector = (ChannelSelector) selectorOptional.get();
                    if (isChannelMatches(ctx, channelSelector) && isEntrypointMatches(ctx, channelSelector)) {
                        return flow;
                    }
                } else {
                    return flow;
                }
                return null;
            }
        );
    }

    private boolean isChannelMatches(final GenericExecutionContext ctx, final ChannelSelector channelSelector) {
        Pattern pattern = pathPatterns.getOrCreate(channelSelector.getChannel());
        String pathInfo = ctx.request().pathInfo();
        return (channelSelector.getChannelOperator() == Operator.EQUALS)
            ? pattern.matcher(pathInfo).matches()
            : pattern.matcher(pathInfo).lookingAt();
    }

    private boolean isEntrypointMatches(final GenericExecutionContext ctx, final ChannelSelector channelSelector) {
        EntrypointConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);

        if (entrypointConnector != null) {
            return (
                isEntrypointIdMatches(channelSelector, entrypointConnector) &&
                isEntrypointModesMatches(channelSelector, entrypointConnector)
            );
        }
        return true;
    }

    private boolean isEntrypointIdMatches(final ChannelSelector channelSelector, final EntrypointConnector entrypointConnector) {
        return (
            channelSelector.getEntrypoints() == null ||
            channelSelector.getEntrypoints().isEmpty() ||
            channelSelector.getEntrypoints().contains(entrypointConnector.id())
        );
    }

    private boolean isEntrypointModesMatches(final ChannelSelector channelSelector, final EntrypointConnector entrypointConnector) {
        return (
            channelSelector.getOperations() == null ||
            channelSelector.getOperations().isEmpty() ||
            channelSelector
                .getOperations()
                .stream()
                .map(operation -> ConnectorMode.fromLabel(operation.getConnectorMode().getLabel()))
                .anyMatch(connectorMode -> entrypointConnector.supportedModes().contains(connectorMode))
        );
    }
}
