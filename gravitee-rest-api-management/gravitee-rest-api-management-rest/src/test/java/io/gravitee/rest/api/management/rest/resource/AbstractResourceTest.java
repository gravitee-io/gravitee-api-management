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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.rest.api.management.rest.JerseySpringTest;
import io.gravitee.rest.api.security.authentication.AuthenticationProvider;
import io.gravitee.rest.api.security.authentication.AuthenticationProviderManager;
import io.gravitee.rest.api.security.cookies.JWTCookieGenerator;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
public abstract class AbstractResourceTest extends JerseySpringTest {

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
    protected ApiService apiService;

    @Autowired
    protected ApplicationService applicationService;

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
    private ApiMetadataService apiMetadataService;

    @Autowired
    protected ParameterService parameterService;

    @Autowired
    protected VirtualHostService virtualHostService;

    @Autowired
    protected ViewService viewService;

    @Autowired
    protected PolicyOperationVisitorManager policyOperationVisitorManager;
    
    @Autowired
    protected ConfigService configService;

    @Autowired
    protected OrganizationService organizationService;

    @Autowired
    protected EnvironmentService environmentService;

    @Configuration
    @PropertySource("classpath:/io/gravitee/rest/api/management/rest/resource/jwt.properties")
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
        public JWTCookieGenerator jwtCookieGenerator() {
            return mock(JWTCookieGenerator.class);
        }

        @Bean
        public TaskService taskService() {
            return mock(TaskService.class);
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
        public ApiMetadataService apiMetadataService() {
    	    return mock(ApiMetadataService.class);
        }

        @Bean
        public VirtualHostService virtualHostService() {
            return mock(VirtualHostService.class);
        }

        @Bean
        public ViewService viewService() {
            return mock(ViewService.class);
        }

        @Bean
        public PolicyOperationVisitorManager policyOperationVisitorManager() {
    	    return mock(PolicyOperationVisitorManager.class);
        }

        @Bean
        public ConfigService configService() {
            return mock(ConfigService.class);
        }

        @Bean
        public OrganizationService organizationService() {
            return mock(OrganizationService.class);
        }

        @Bean
        public EnvironmentService environmentService() {
            return mock(EnvironmentService.class);
        }
    }
}
