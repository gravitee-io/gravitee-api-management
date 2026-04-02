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
package io.gravitee.apim.infra.domain_service.subscription_form;

import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormExpressionResolverDomainService;
import io.gravitee.el.TemplateEngine;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import org.springframework.stereotype.Service;

@Service
@CustomLog
public class GraviteeElSubscriptionFormExpressionResolverDomainServiceImpl implements SubscriptionFormExpressionResolverDomainService {

    @Override
    public List<String> resolveToOptions(String expression, Map<String, Object> templateParams) {
        try {
            var templateEngine = TemplateEngine.templateEngine();
            templateParams.forEach((key, value) -> templateEngine.getTemplateContext().setVariable(key, value));

            String resolved = templateEngine.evalNow(expression, String.class);
            if (resolved == null || resolved.isBlank()) {
                return List.of();
            }

            return Arrays.stream(resolved.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        } catch (Exception e) {
            log.warn("Failed to resolve EL expression '{}' for subscription form options", expression, e);
            return List.of();
        }
    }
}
