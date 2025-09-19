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

import static io.gravitee.repository.management.model.ApiKeyMode.*;
import static io.gravitee.repository.management.model.Plan.PlanSecurityType.*;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.Subscription;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationApiKeyModeUpgraderTest {

    @InjectMocks
    private ApplicationApiKeyModeUpgrader upgrader;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Test(expected = UpgraderException.class)
    public void upgrade_should_failed_because_of_exception() throws TechnicalException, UpgraderException {
        when(planRepository.findAll()).thenThrow(new RuntimeException());

        upgrader.upgrade();

        verify(planRepository, times(1)).findAll();
        verifyNoMoreInteractions(planRepository);
    }

    @Test
    public void upgrade_should_set_apiKeyMode_of_applications_regarding_apiKeys_subscriptions() throws Exception {
        when(planRepository.findAll()).thenReturn(
            Set.of(
                buildTestPlan("plan-1", API_KEY),
                buildTestPlan("plan-2", KEY_LESS),
                buildTestPlan("plan-3", OAUTH2),
                buildTestPlan("plan-4", API_KEY)
            )
        );

        when(subscriptionRepository.findAll()).thenReturn(
            Set.of(
                // application-1 has 1 subscription to 3 plans, but only 1 API Key
                buildTestSubscription("plan-1", "application-1"),
                buildTestSubscription("plan-2", "application-1"),
                buildTestSubscription("plan-3", "application-1"),
                // application-2 has 1 subscription to 3 plans, including 2 API Key plans
                buildTestSubscription("plan-1", "application-2"),
                buildTestSubscription("plan-2", "application-2"),
                buildTestSubscription("plan-4", "application-2"),
                // application-3 has 1 subscription on key less plan only
                buildTestSubscription("plan-2", "application-3"),
                // application-4 has 2 subscription to API Key plans
                buildTestSubscription("plan-1", "application-4"),
                buildTestSubscription("plan-4", "application-4"),
                // application-4 has 2 subscription to API Key plans
                buildTestSubscription("plan-1", "application-6"),
                buildTestSubscription("plan-4", "application-6")
            )
        );

        when(applicationRepository.findAll()).thenReturn(
            Set.of(
                buildTestApplication("application-1"),
                buildTestApplication("application-2"),
                buildTestApplication("application-3"),
                buildTestApplication("application-4"),
                buildTestApplication("application-5"),
                buildTestApplication("application-6", SHARED) // application-6 won't be updated as its API Key mode is already defined
            )
        );

        upgrader.upgrade();

        // all applications have been searched
        verify(applicationRepository, times(1)).findAll();
        // application-2 and application-4 API Key mode are updated to EXCLUSIVE
        verify(applicationRepository, times(1)).update(argThat(a -> a.getId().equals("application-2") && a.getApiKeyMode() == EXCLUSIVE));
        verify(applicationRepository, times(1)).update(argThat(a -> a.getId().equals("application-4") && a.getApiKeyMode() == EXCLUSIVE));
        // 3 others applications API Key mode are set to UNSPECIFIED
        verify(applicationRepository, times(1)).update(argThat(a -> a.getId().equals("application-1") && a.getApiKeyMode() == UNSPECIFIED));
        verify(applicationRepository, times(1)).update(argThat(a -> a.getId().equals("application-3") && a.getApiKeyMode() == UNSPECIFIED));
        verify(applicationRepository, times(1)).update(argThat(a -> a.getId().equals("application-5") && a.getApiKeyMode() == UNSPECIFIED));
        // nothing more has been done
        verifyNoMoreInteractions(applicationRepository);
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.APPLICATION_API_KEY_MODE_UPGRADER, upgrader.getOrder());
    }

    private Plan buildTestPlan(String id, Plan.PlanSecurityType securityType) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setSecurity(securityType);
        return plan;
    }

    private Subscription buildTestSubscription(String planId, String applicationId) {
        Subscription subscription = new Subscription();
        subscription.setId(planId + applicationId);
        subscription.setPlan(planId);
        subscription.setApplication(applicationId);
        return subscription;
    }

    private Application buildTestApplication(String id) {
        return buildTestApplication(id, null);
    }

    private Application buildTestApplication(String id, ApiKeyMode apiKeyMode) {
        Application application = new Application();
        application.setId(id);
        application.setApiKeyMode(apiKeyMode);
        return application;
    }
}
