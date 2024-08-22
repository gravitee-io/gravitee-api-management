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
package io.gravitee.rest.api.portal.rest.resource;

import static org.mockito.Mockito.reset;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.rest.api.portal.rest.JerseySpringTest;
import io.gravitee.rest.api.portal.rest.mapper.AnalyticsMapper;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.portal.rest.mapper.CategoryMapper;
import io.gravitee.rest.api.portal.rest.mapper.ConfigurationMapper;
import io.gravitee.rest.api.portal.rest.mapper.IdentityProviderMapper;
import io.gravitee.rest.api.portal.rest.mapper.KeyMapper;
import io.gravitee.rest.api.portal.rest.mapper.LogMapper;
import io.gravitee.rest.api.portal.rest.mapper.MemberMapper;
import io.gravitee.rest.api.portal.rest.mapper.PageMapper;
import io.gravitee.rest.api.portal.rest.mapper.PlanMapper;
import io.gravitee.rest.api.portal.rest.mapper.PortalMenuLinkMapper;
import io.gravitee.rest.api.portal.rest.mapper.PortalNotificationMapper;
import io.gravitee.rest.api.portal.rest.mapper.RatingMapper;
import io.gravitee.rest.api.portal.rest.mapper.ReferenceMetadataMapper;
import io.gravitee.rest.api.portal.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.portal.rest.mapper.ThemeMapper;
import io.gravitee.rest.api.portal.rest.mapper.TicketMapper;
import io.gravitee.rest.api.portal.rest.mapper.UserMapper;
import io.gravitee.rest.api.portal.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.security.authentication.AuthenticationProvider;
import io.gravitee.rest.api.security.authentication.AuthenticationProviderManager;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.AccessControlService;
import io.gravitee.rest.api.service.AnalyticsService;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationMetadataService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.CustomUserFieldService;
import io.gravitee.rest.api.service.EntrypointService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.FetcherService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.HealthCheckService;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.LogsService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.MessageService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.QualityMetricsService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.TaskService;
import io.gravitee.rest.api.service.ThemeService;
import io.gravitee.rest.api.service.TicketService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.filtering.FilteringService;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ResourceContextConfiguration.class })
public abstract class AbstractResourceTest extends JerseySpringTest {

    @Autowired
    protected CustomUserFieldService customUserFieldService;

    @Autowired
    protected ApiService apiService;

    @Autowired
    protected ApiSearchService apiSearchService;

    @Autowired
    protected ApiEntrypointService apiEntrypointService;

    @Autowired
    protected ApiAuthorizationService apiAuthorizationService;

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
    protected ParameterService parameterService;

    @Autowired
    protected ApiMetadataService metadataService;

    @Autowired
    protected PlanService planService;

    @Autowired
    protected PlanSearchService planSearchService;

    @Autowired
    protected SubscriptionService subscriptionService;

    @Autowired
    protected EntrypointService entrypointService;

    @Autowired
    protected ApiKeyService apiKeyService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected LogsService logsService;

    @Autowired
    protected AnalyticsService analyticsService;

    @Autowired
    protected PortalNotificationConfigService portalNotificationConfigService;

    @Autowired
    protected PortalNotificationService portalNotificationService;

    @Autowired
    protected GenericNotificationConfigService genericNotificationConfigService;

    @Autowired
    protected TopApiService topApiService;

    @Autowired
    protected CategoryService categoryService;

    @Autowired
    protected ApiCategoryService apiCategoryService;

    @Autowired
    protected TicketService ticketService;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected CookieGenerator cookieGenerator;

    @Autowired
    protected ApiMapper apiMapper;

    @Autowired
    protected PageMapper pageMapper;

    @Autowired
    protected PlanMapper planMapper;

    @Autowired
    protected RatingMapper ratingMapper;

    @Autowired
    protected SubscriptionMapper subscriptionMapper;

    @Autowired
    protected KeyMapper keyMapper;

    @Autowired
    protected ApplicationMapper applicationMapper;

    @Autowired
    protected MemberMapper memberMapper;

    @Autowired
    protected UserMapper userMapper;

    @Autowired
    protected LogMapper logMapper;

    @Autowired
    protected AnalyticsMapper analyticsMapper;

    @Autowired
    protected CategoryMapper categoryMapper;

    @Autowired
    protected TicketMapper ticketMapper;

    @Autowired
    protected ConfigurationMapper configMapper;

    @Autowired
    protected PortalNotificationMapper portalNotificationMapper;

    @Autowired
    protected IdentityProviderMapper identityProviderMapper;

    @Autowired
    protected HealthCheckService healthCheckService;

    @Autowired
    protected IdentityService identityService;

    @Autowired
    protected FilteringService filteringService;

    @Autowired
    protected ApplicationMetadataService applicationMetadataService;

    @Autowired
    protected ReferenceMetadataMapper referenceMetadataMapper;

    @Autowired
    protected IdentityProviderActivationService identityProviderActivationService;

    @Autowired
    protected EnvironmentService environmentService;

    @Autowired
    protected AccessControlService accessControlService;

    @Autowired
    private AuthoritiesProvider authoritiesProvider;

    @Autowired
    protected ThemeService themeService;

    @Autowired
    protected ThemeMapper themeMapper;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected CategoryDomainService categoryDomainService;

    public AbstractResourceTest() {
        super(
            new AuthenticationProviderManager() {
                @Override
                public List<AuthenticationProvider> getIdentityProviders() {
                    return Collections.emptyList();
                }

                @Override
                public Optional<AuthenticationProvider> findIdentityProviderByType(String type) {
                    return Optional.empty();
                }
            }
        );
    }

    public AbstractResourceTest(AuthenticationProviderManager authenticationProviderManager) {
        super(authenticationProviderManager);
    }

    protected void resetAllMocks() {
        reset(apiService);
        reset(apiSearchService);
        reset(apiAuthorizationService);
        reset(apiEntrypointService);
        reset(applicationService);
        reset(policyService);
        reset(userService);
        reset(fetcherService);
        reset(swaggerService);
        reset(membershipService);
        reset(roleService);
        reset(pageService);
        reset(groupService);
        reset(ratingService);
        reset(permissionService);
        reset(notifierService);
        reset(qualityMetricsService);
        reset(messageService);
        reset(socialIdentityProviderService);
        reset(tagService);
        reset(parameterService);
        reset(metadataService);
        reset(planService);
        reset(planSearchService);
        reset(subscriptionService);
        reset(entrypointService);
        reset(apiKeyService);
        reset(taskService);
        reset(logsService);
        reset(analyticsService);
        reset(portalNotificationConfigService);
        reset(portalNotificationService);
        reset(genericNotificationConfigService);
        reset(topApiService);
        reset(categoryService);
        reset(apiCategoryService);
        reset(ticketService);
        reset(configService);
        reset(authenticationProvider);
        reset(cookieGenerator);
        reset(apiMapper);
        reset(pageMapper);
        reset(planMapper);
        reset(ratingMapper);
        reset(subscriptionMapper);
        reset(keyMapper);
        reset(subscriptionMapper);
        reset(applicationMapper);
        reset(memberMapper);
        reset(userMapper);
        reset(logMapper);
        reset(analyticsMapper);
        reset(portalNotificationMapper);
        reset(categoryMapper);
        reset(ticketMapper);
        reset(configMapper);
        reset(identityProviderMapper);
        reset(healthCheckService);
        reset(applicationTypeService);
        reset(identityService);
        reset(filteringService);
        reset(applicationMetadataService);
        reset(referenceMetadataMapper);
        reset(customUserFieldService);
        reset(identityProviderActivationService);
        reset(authenticationProvider);
        reset(environmentService);
        reset(accessControlService);
        reset(themeService);
        reset(themeMapper);
    }

    @Priority(50)
    public static class NotAuthenticatedAuthenticationFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(
                new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return null;
                    }

                    @Override
                    public boolean isUserInRole(String string) {
                        return false;
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
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
