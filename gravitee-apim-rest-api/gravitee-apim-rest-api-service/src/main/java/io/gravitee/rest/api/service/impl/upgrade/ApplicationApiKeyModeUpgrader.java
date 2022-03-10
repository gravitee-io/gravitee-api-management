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
package io.gravitee.rest.api.service.impl.upgrade;

import static io.gravitee.repository.management.model.ApiKeyMode.EXCLUSIVE;
import static io.gravitee.repository.management.model.ApiKeyMode.UNSPECIFIED;
import static io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY;
import static java.util.stream.Collectors.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.service.InstallationService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This one-shot upgrader initializes all applications api_key_mode to :
 * - EXCLUSIVE if it has more than 1 subscription to an API key plan
 * - UNSPECIFIED otherwise
 *
 * @author GraviteeSource Team
 */
@Component
public class ApplicationApiKeyModeUpgrader extends OneShotUpgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationApiKeyModeUpgrader.class);

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public ApplicationApiKeyModeUpgrader() {
        super(InstallationService.APPLICATION_API_KEY_MODE_UPGRADER_STATUS);
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    protected void processOneShotUpgrade() throws Exception {
        List<String> apiKeyPlansIds = findAllApiKeyPlansId();
        Map<String, Long> apiKeySubscriptionsCountByApplication = countApiKeySubscriptionsByApplication(apiKeyPlansIds);

        applicationRepository
            .findAll()
            .forEach(
                application -> {
                    if (application.getApiKeyMode() == null || application.getApiKeyMode() == UNSPECIFIED) {
                        Long apiKeysSubscriptionCount = apiKeySubscriptionsCountByApplication.getOrDefault(application.getId(), 0L);
                        updateApplicationApiKeyMode(application, apiKeysSubscriptionCount > 1 ? EXCLUSIVE : UNSPECIFIED);
                    }
                }
            );
    }

    private List<String> findAllApiKeyPlansId() throws TechnicalException {
        return planRepository.findAll().stream().filter(p -> p.getSecurity() == API_KEY).map(Plan::getId).collect(toList());
    }

    private Map<String, Long> countApiKeySubscriptionsByApplication(List<String> apiKeyPlansIds) throws TechnicalException {
        return subscriptionRepository
            .findAll()
            .stream()
            .filter(s -> apiKeyPlansIds.contains(s.getPlan()))
            .collect(groupingBy(Subscription::getApplication, counting()));
    }

    private void updateApplicationApiKeyMode(Application application, ApiKeyMode apiKeyMode) {
        application.setApiKeyMode(apiKeyMode);
        try {
            applicationRepository.update(application);
        } catch (TechnicalException e) {
            LOGGER.error("Failed to set EXCLUSIVE ApiKeyMode to application {}", application, e);
        }
    }
}
