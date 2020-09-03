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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.rest.api.portal.rest.JerseySpringTest;
import io.gravitee.rest.api.portal.rest.mapper.*;
import io.gravitee.rest.api.security.authentication.AuthenticationProvider;
import io.gravitee.rest.api.security.authentication.AuthenticationProviderManager;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.filtering.FilteringService;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public abstract class AbstractResourceTest extends JerseySpringTest {

    protected void resetAllMocks() {
        reset(apiService);
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
    }

    public AbstractResourceTest() {
        super(new AuthenticationProviderManager() {
            @Override
            public List<AuthenticationProvider> getIdentityProviders() {
                return Collections.emptyList();
            }

            @Override
            public Optional<AuthenticationProvider> findIdentityProviderByType(String type) {
                return Optional.empty();
            }
        });
    }

    public AbstractResourceTest(AuthenticationProviderManager authenticationProviderManager) {
        super(authenticationProviderManager);
    }

    @Autowired
    protected CustomUserFieldService customUserFieldService;

    @Autowired
    protected ApiService apiService;

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

    @Configuration
    @PropertySource("classpath:/io/gravitee/rest/api/portal/rest/resource/jwt.properties")
    static class ContextConfiguration {

        @Bean
        public ApiService apiService() {
            return mock(ApiService.class);
        }

        @Bean
        public ApplicationService applicationService() {
            return mock(ApplicationService.class);
        }

        @Bean
        public ApplicationTypeService applicationTypeService() {
            return mock(ApplicationTypeService.class);
        }

        @Bean
        public UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        public PolicyService policyService() {
            return mock(PolicyService.class);
        }

        @Bean
        public FetcherService fetcherService() {
            return mock(FetcherService.class);
        }

        @Bean
        public SwaggerService swaggerService() {
            return mock(SwaggerService.class);
        }

        @Bean
        public MembershipService membershipService() {
            return mock(MembershipService.class);
        }

        @Bean
        public RoleService roleService() {
            return mock(RoleService.class);
        }

        @Bean("oauth2")
        public AuthenticationProvider authenticationProvider() {
            return mock(AuthenticationProvider.class);
        }

        @Bean
        public PageService pageService() {
            return mock(PageService.class);
        }

        @Bean
        public GroupService groupService() {
            return mock(GroupService.class);
        }

        @Bean
        public RatingService ratingService() {
            return mock(RatingService.class);
        }

        @Bean
        public PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        public NotifierService notifierService() {
            return mock(NotifierService.class);
        }

        @Bean
        public TopApiService topApiService() {
            return mock(TopApiService.class);
        }

        @Bean
        public CookieGenerator jwtCookieGenerator() {
            return mock(CookieGenerator.class);
        }

        @Bean
        public TaskService taskService() {
            return mock(TaskService.class);
        }

        @Bean
        public LogsService logsService() {
            return mock(LogsService.class);
        }

        @Bean
        public QualityMetricsService qualityMetricsService() {
            return mock(QualityMetricsService.class);
        }

        @Bean
        public MessageService messageService() {
            return mock(MessageService.class);
        }

        @Bean
        public SocialIdentityProviderService socialIdentityProviderService() {
            return mock(SocialIdentityProviderService.class);
        }

        @Bean
        public TagService tagService() {
            return mock(TagService.class);
        }

        @Bean
        public MediaService mediaService() {
            return mock(MediaService.class);
        }

        @Bean
        public ParameterService parameterService() {
            return mock(ParameterService.class);
        }

        @Bean
        public ApiMetadataService metadataService() {
            return mock(ApiMetadataService.class);
        }

        @Bean
        public PlanService planService() {
            return mock(PlanService.class);
        }

        @Bean
        public SubscriptionService subscriptionService() {
            return mock(SubscriptionService.class);
        }

        @Bean
        public EntrypointService entrypointService() {
            return mock(EntrypointService.class);
        }

        @Bean
        public ApiKeyService apiKeyService() {
            return mock(ApiKeyService.class);
        }

        @Bean
        public AnalyticsService analyticsService() {
            return mock(AnalyticsService.class);
        }

        @Bean
        public PortalNotificationConfigService portalNotificationConfigService() {
            return mock(PortalNotificationConfigService.class);
        }

        @Bean
        public PortalNotificationService portalNotificationService() {
            return mock(PortalNotificationService.class);
        }

        @Bean
        public GenericNotificationConfigService genericNotificationConfigService() {
            return mock(GenericNotificationConfigService.class);
        }

        @Bean
        public CategoryService categoryService() {
            return mock(CategoryService.class);
        }

        @Bean
        public TicketService ticketService() {
            return mock(TicketService.class);
        }

        @Bean
        public ConfigService configService() {
            return mock(ConfigService.class);
        }

        @Bean
        public ApiMapper apiMapper() {
            return mock(ApiMapper.class);
        }

        @Bean
        public PageMapper pageMapper() {
            return mock(PageMapper.class);
        }

        @Bean
        public PlanMapper planMapper() {
            return mock(PlanMapper.class);
        }

        @Bean
        public RatingMapper ratingMapper() {
            return mock(RatingMapper.class);
        }

        @Bean
        public SubscriptionMapper subscriptionMapper() {
            return mock(SubscriptionMapper.class);
        }

        @Bean
        public KeyMapper keyMapper() {
            return mock(KeyMapper.class);
        }

        @Bean
        public ApplicationMapper applicationMapper() {
            return mock(ApplicationMapper.class);
        }

        @Bean
        public MemberMapper memberMapper() {
            return mock(MemberMapper.class);
        }

        @Bean
        public UserMapper userMapper() {
            return mock(UserMapper.class);
        }

        @Bean
        public LogMapper logMapper() {
            return mock(LogMapper.class);
        }

        @Bean
        public AnalyticsMapper analyticsMapper() {
            return mock(AnalyticsMapper.class);
        }

        @Bean
        public PortalNotificationMapper portalNotificationMapper() {
            return mock(PortalNotificationMapper.class);
        }

        @Bean
        public CategoryMapper categoryMapper() {
            return mock(CategoryMapper.class);
        }

        @Bean
        public TicketMapper ticketMapper() {
            return mock(TicketMapper.class);
        }

        @Bean
        public ConfigurationMapper configMapper() {
            return mock(ConfigurationMapper.class);
        }

        @Bean
        public IdentityProviderMapper identityProviderMapper() {
            return mock(IdentityProviderMapper.class);
        }

        @Bean
        public HealthCheckService healthCheckService() {
            return mock(HealthCheckService.class);
        }

        @Bean
        public IdentityService identityService() {
            return mock(IdentityService.class);
        }

        @Bean
        public FilteringService filteringService() {
            return mock(FilteringService.class);
        }

        @Bean
        public ApplicationMetadataService applicationMetadataService() {
            return mock(ApplicationMetadataService.class);
        }

        @Bean
        public ReferenceMetadataMapper referenceMetadataMapper() {
            return mock(ReferenceMetadataMapper.class);
        }

        @Bean
        public CustomUserFieldService customUserFieldService() {
            return mock(CustomUserFieldService.class);
        }
    }
}
