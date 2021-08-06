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
package io.gravitee.rest.api.management.rest.filter;

import static org.mockito.Mockito.mock;

import io.gravitee.rest.api.service.*;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.SecurityContext;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public abstract class AbstractFilterTest {

    @Autowired
    protected ApiService apiService;

    @Autowired
    protected ApplicationService applicationService;

    @Autowired
    protected ResourceInfo resourceInfo;

    @Autowired
    protected SecurityContext securityContext;

    @Autowired
    protected MembershipService membershipService;

    @Autowired
    protected RoleService roleService;

    @Configuration
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
        public SecurityContext securityContext() {
            return mock(SecurityContext.class);
        }

        @Bean
        public ResourceInfo resourceInfo() {
            return mock(ResourceInfo.class);
        }

        @Bean
        public MembershipService membershipService() {
            return mock(MembershipService.class);
        }

        @Bean
        public RoleService roleService() {
            return mock(RoleService.class);
        }
    }
}
