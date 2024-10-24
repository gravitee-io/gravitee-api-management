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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractApiSubscriptionsResourceTest extends AbstractResourceTest {

    protected static final String API = "my-api";
    protected static final String PLAN = "my-plan";
    protected static final String APPLICATION = "my-application";
    protected static final String SUBSCRIPTION = "my-subscription";
    protected static final String SUBSCRIBED_BY = "my-subscriber-id";
    protected static final String ENVIRONMENT = "my-env";

    @Autowired
    protected SubscriptionService subscriptionService;

    @Autowired
    protected PlanSearchService planSearchService;

    @Autowired
    protected ApplicationService applicationService;

    @Autowired
    protected ApiKeyService apiKeyService;

    @Autowired
    protected UserService userService;

    @BeforeEach
    public void init() throws TechnicalException {
        Mockito.reset(subscriptionService, applicationService, planSearchService, parameterService, apiKeyService);
        GraviteeContext.cleanContext();

        Api api = new Api();
        api.setId(API);
        api.setEnvironmentId(ENVIRONMENT);
        when(apiRepository.findById(API)).thenReturn(Optional.of(api));

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
    }
}
