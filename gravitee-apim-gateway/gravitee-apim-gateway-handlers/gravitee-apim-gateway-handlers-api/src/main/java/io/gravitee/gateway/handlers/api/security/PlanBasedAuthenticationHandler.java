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
package io.gravitee.gateway.handlers.api.security;

import static io.gravitee.gateway.security.core.AuthenticationContext.ATTR_INTERNAL_LAST_SECURITY_HANDLER_SUPPORTING_SAME_TOKEN_TYPE;
import static io.gravitee.gateway.security.core.AuthenticationContext.ATTR_INTERNAL_TOKEN_IDENTIFIED;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.el.EvaluableRequest;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class PlanBasedAuthenticationHandler implements AuthenticationHandler {

    private static final String CONTEXT_ATTRIBUTE_PLAN_SELECTION_RULE_BASED =
        ExecutionContext.ATTR_PREFIX + ExecutionContext.ATTR_PLAN + ".selection.rule.based";
    private static final String EXPRESSION_REGEX = "\\{([^#|T|(])";
    private static final String EXPRESSION_REGEX_SUBSTITUTE = "{'{'}$1";
    private static final Logger LOGGER = LoggerFactory.getLogger(PlanBasedAuthenticationHandlerEnhancer.class);

    protected final AuthenticationHandler handler;
    protected final Plan plan;

    /**
     * Check if the incoming request can be handled, searching for a relevant valid subscription.
     *
     * @param authenticationContext
     * @return true is it can handle the incoming request
     */
    protected abstract boolean preCheckSubscription(AuthenticationContext authenticationContext);

    public PlanBasedAuthenticationHandler(final AuthenticationHandler handler, final Plan plan) {
        this.handler = handler;
        this.plan = plan;
    }

    @Override
    public boolean canHandle(AuthenticationContext authenticationContext) {
        return (
            handler.canHandle(authenticationContext) &&
            canHandleSelectionRule(authenticationContext) &&
            preCheckSubscription(authenticationContext)
        );
    }

    /**
     * Check if the incoming request can be handled, according to selection rule.
     * If a selection rule is set on plan, check if it's truthy.
     *
     * @param authenticationContext
     * @return true if selection rule matches the incoming request, or there is no selection rule.
     */
    protected boolean canHandleSelectionRule(AuthenticationContext authenticationContext) {
        String selectionRule = plan.getSelectionRule();
        if (selectionRule != null && !selectionRule.isEmpty()) {
            try {
                Expression expression = new SpelExpressionParser()
                    .parseExpression(selectionRule.replaceAll(EXPRESSION_REGEX, EXPRESSION_REGEX_SUBSTITUTE));

                StandardEvaluationContext evaluation = new StandardEvaluationContext();
                evaluation.setVariable("request", new EvaluableRequest(authenticationContext.request()));
                evaluation.setVariable("context", new EvaluableAuthenticationContext(authenticationContext));

                Boolean value = expression.getValue(evaluation, Boolean.class);
                // Remove any security  token as the selection rule don't match
                if (Boolean.FALSE.equals(value)) {
                    authenticationContext.setInternalAttribute(ATTR_INTERNAL_TOKEN_IDENTIFIED, false);
                }

                // Set as Last handler (jwt or oauth), no need to check the subscription,
                // let the CheckSubscriptionPolicy do the job and return an appropriate error.
                authenticationContext.setInternalAttribute(ATTR_INTERNAL_LAST_SECURITY_HANDLER_SUPPORTING_SAME_TOKEN_TYPE, true);
                return Boolean.TRUE.equals(value);
            } catch (ParseException | EvaluationException e) {
                LOGGER.error("Plan selection rule execution failed", e);
                return false;
            }
        }
        return true;
    }

    @Override
    public String name() {
        return handler.name();
    }

    @Override
    public int order() {
        return handler.order();
    }

    @Override
    public List<AuthenticationPolicy> handle(ExecutionContext executionContext) {
        executionContext.setAttribute(ExecutionContext.ATTR_PLAN, plan.getId());
        executionContext.setAttribute(
            CONTEXT_ATTRIBUTE_PLAN_SELECTION_RULE_BASED,
            plan.getSelectionRule() != null && !plan.getSelectionRule().isEmpty()
        );

        return handler
            .handle(executionContext)
            .stream()
            .map(securityPolicy -> {
                // Override the configuration of the policy with the one provided by the plan
                if (securityPolicy instanceof PluginAuthenticationPolicy) {
                    return new PluginAuthenticationPolicy() {
                        @Override
                        public String name() {
                            return ((PluginAuthenticationPolicy) securityPolicy).name();
                        }

                        @Override
                        public String configuration() {
                            return plan.getSecurityDefinition();
                        }
                    };
                }

                return securityPolicy;
            })
            .collect(Collectors.toList());
    }

    @Override
    public String tokenType() {
        return handler.tokenType();
    }
}
