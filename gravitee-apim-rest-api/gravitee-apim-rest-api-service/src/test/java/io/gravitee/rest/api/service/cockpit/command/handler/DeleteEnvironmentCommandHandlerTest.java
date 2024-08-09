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
package io.gravitee.rest.api.service.cockpit.command.handler;

import static org.junit.Assert.assertEquals;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.api.ApiHeaderRepository;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApplicationAlertService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeleteEnvironmentCommandHandlerTest {

    @Mock
    private AccessPointRepository accessPointRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApiHeaderRepository apiHeaderRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private FlowRepository flowRepository;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private ParameterRepository parameterRepository;

    @Mock
    private PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Mock
    private ApiCategoryOrderRepository apiCategoryOrderRepository;

    @Mock
    private ApiQualityRuleRepository apiQualityRuleRepository;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MetadataRepository metadataRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ApiStateService apiStateService;

    @Mock
    private EventService eventService;

    @Mock
    private AccessPointCrudService accessPointService;

    @Mock
    private IdentityProviderActivationService identityProviderActivationService;

    @Mock
    private DictionaryService dictionaryService;

    @Mock
    private GenericNotificationConfigService genericNotificationConfigService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private ApplicationAlertService applicationAlertService;

    @Mock
    private SearchEngineService searchEngineService;

    private DeleteEnvironmentCommandHandler cut;

    @Before
    public void setUp() throws Exception {
        cut =
            new DeleteEnvironmentCommandHandler(
                accessPointRepository,
                apiRepository,
                apiHeaderRepository,
                applicationRepository,
                subscriptionRepository,
                apiKeyRepository,
                planRepository,
                flowRepository,
                pageRepository,
                parameterRepository,
                portalNotificationConfigRepository,
                apiCategoryOrderRepository,
                apiQualityRuleRepository,
                auditRepository,
                mediaRepository,
                metadataRepository,
                alertService,
                environmentService,
                apiStateService,
                eventService,
                accessPointService,
                identityProviderActivationService,
                dictionaryService,
                genericNotificationConfigService,
                membershipService,
                applicationAlertService,
                searchEngineService
            );
    }

    @Test
    public void supportType() {
        assertEquals(CockpitCommandType.DELETE_ENVIRONMENT.name(), cut.supportType());
    }
}
