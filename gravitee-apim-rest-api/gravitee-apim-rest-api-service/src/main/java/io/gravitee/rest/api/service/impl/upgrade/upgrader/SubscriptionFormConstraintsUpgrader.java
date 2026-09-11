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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.SUBSCRIPTION_FORM_CONSTRAINTS_UPGRADER;

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormConstraintsFactory;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSchemaGenerator;
import io.gravitee.apim.infra.adapter.SubscriptionFormAdapter;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Upgrader that generates and persists validation constraints for existing subscription forms
 * that were created before constraint generation was introduced.
 *
 * <p>Forms whose {@code validationConstraints} is the empty-object sentinel {@code "{}"} are processed:
 * the GMD content is parsed, constraints are derived, and the result (or {@code "{}"} when the form has
 * no constrained fields) is stored. Forms that already carry real constraints are skipped.</p>
 *
 * @author Gravitee.io Team
 */
@Component
@CustomLog
public class SubscriptionFormConstraintsUpgrader implements Upgrader {

    private final SubscriptionFormRepository subscriptionFormRepository;
    private final SubscriptionFormSchemaGenerator schemaGenerator;

    public SubscriptionFormConstraintsUpgrader(
        @Lazy SubscriptionFormRepository subscriptionFormRepository,
        @Lazy SubscriptionFormSchemaGenerator schemaGenerator
    ) {
        this.subscriptionFormRepository = subscriptionFormRepository;
        this.schemaGenerator = schemaGenerator;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        var forms = subscriptionFormRepository.findAll();
        int updated = 0;

        for (var form : forms) {
            if (!form.getValidationConstraints().equals("{}")) {
                continue;
            }
            try {
                var schema = schemaGenerator.generate(GraviteeMarkdown.of(form.getGmdContent()));
                var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
                String json = SubscriptionFormAdapter.writeFieldConstraintsJson(constraints);
                form.setValidationConstraints(json);
                subscriptionFormRepository.update(form);
                updated++;
            } catch (Exception e) {
                log.error("Failed to generate validation constraints for subscription form [{}]", form.getId(), e);
            }
        }

        log.info("Subscription form constraints upgrader completed. Generated constraints for {}/{} forms.", updated, forms.size());
        return true;
    }

    @Override
    public int getOrder() {
        return SUBSCRIPTION_FORM_CONSTRAINTS_UPGRADER;
    }
}
