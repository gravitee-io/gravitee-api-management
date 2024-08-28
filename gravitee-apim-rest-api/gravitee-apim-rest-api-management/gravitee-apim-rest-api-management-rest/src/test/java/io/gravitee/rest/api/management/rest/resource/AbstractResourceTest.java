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
package io.gravitee.rest.api.management.rest.resource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.debug.use_case.DebugApiUseCase;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.rest.api.management.rest.JerseySpringTest;
import io.gravitee.rest.api.management.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.security.authentication.AuthenticationProvider;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.configuration.application.ClientRegistrationService;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.gravitee.rest.api.service.configuration.spel.SpelService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiGroupService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ResourceContextConfiguration.class })
public abstract class AbstractResourceTest extends JerseySpringTest {

    @Autowired
    protected ApiService apiService;

    @Autowired
    protected ApiCRDService apiCRDService;

    @Autowired
    protected ApiValidationService apiValidationService;

    @Autowired
    protected io.gravitee.rest.api.service.v4.ApiService apiServiceV4;

    @Autowired
    protected io.gravitee.rest.api.service.v4.ApiSearchService apiSearchServiceV4;

    @Autowired
    protected ApiAuthorizationService apiAuthorizationService;

    @Autowired
    protected io.gravitee.rest.api.service.v4.ApiAuthorizationService apiAuthorizationServiceV4;

    @Autowired
    protected ApiGroupService apiGroupService;

    @Autowired
    protected ApplicationService applicationService;

    @Autowired
    protected ApplicationTypeService applicationTypeService;

    @Autowired
    protected PolicyService policyService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected FetcherService fetcherService;

    @Autowired
    protected SwaggerService swaggerService;

    @Autowired
    protected MembershipService membershipService;

    @Autowired
    protected RoleService roleService;

    @Autowired
    @Qualifier("oauth2")
    protected AuthenticationProvider authenticationProvider;

    @Autowired
    protected PageService pageService;

    @Autowired
    protected GroupService groupService;

    @Autowired
    protected RatingService ratingService;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected NotifierService notifierService;

    @Autowired
    protected QualityMetricsService qualityMetricsService;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected SocialIdentityProviderService socialIdentityProviderService;

    @Autowired
    protected TagService tagService;

    @Autowired
    protected ApiMetadataService apiMetadataService;

    @Autowired
    protected ApplicationMetadataService applicationMetadataService;

    @Autowired
    protected ParameterService parameterService;

    @Autowired
    protected VerifyApiPathDomainService verifyApiPathDomainService;

    @Autowired
    protected CategoryService categoryService;

    @Autowired
    protected PolicyOperationVisitorManager policyOperationVisitorManager;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected OrganizationService organizationService;

    @Autowired
    protected EnvironmentService environmentService;

    @Autowired
    protected CustomUserFieldService customUserFieldService;

    @Autowired
    protected AuthoritiesProvider authoritiesProvider;

    @Autowired
    protected DictionaryService dictionaryService;

    @Autowired
    protected TicketService ticketService;

    @Autowired
    protected PlanService planService;

    @Autowired
    protected PlanSearchService planSearchService;

    @Autowired
    protected SubscriptionService subscriptionService;

    @Autowired
    protected IdentityProviderActivationService identityProviderActivationService;

    @Autowired
    protected IdentityProviderService identityProviderService;

    @Autowired
    protected ApiKeyService apiKeyService;

    @Autowired
    protected ClientRegistrationService clientRegistrationService;

    @Autowired
    protected SpelService spelService;

    @Autowired
    protected AnalyticsService analyticsService;

    @Autowired
    protected InstallationService installationService;

    @Autowired
    protected SearchEngineService searchEngineService;

    @Autowired
    protected GroupRepository groupRepository;

    @Autowired
    protected AccessControlService accessControlService;

    @Autowired
    protected EventManager eventManager;

    @Autowired
    protected PromotionService promotionService;

    @Autowired
    protected ApiDuplicatorService apiDuplicatorService;

    @Autowired
    protected DebugApiUseCase debugApiUseCase;

    @Autowired
    protected TokenService tokenService;

    @Autowired
    protected ApiExportService apiExportService;

    @Autowired
    protected ApiConverter apiConverter;

    @Autowired
    protected CategoryMapper categoryMapper;

    @Autowired
    protected AlertService alertService;

    @Autowired
    protected AlertAnalyticsService alertAnalyticsService;

    @Autowired
    protected JsonPatchService jsonPatchService;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AuditService auditService;

    @Autowired
    protected WorkflowService workflowService;

    @Autowired
    protected LogsService logsService;

    @Autowired
    protected MediaService mediaService;

    @Autowired
    protected ApiDefinitionContextService definitionContextService;

    @Autowired
    protected ThemeService themeService;

    @Autowired
    protected CategoryDomainService categoryDomainService;

    @Before
    public void setUp() throws Exception {
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                any(RolePermission.class),
                anyString(),
                any(RolePermissionAction[].class)
            )
        )
            .thenReturn(true);
    }

    @Priority(50)
    public static class NotAdminAuthenticationFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(
                new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return () -> USER_NAME;
                    }

                    @Override
                    public boolean isUserInRole(String string) {
                        return false;
                    }

                    @Override
                    public boolean isSecure() {
                        return true;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return "BASIC";
                    }
                }
            );
        }
    }
}
