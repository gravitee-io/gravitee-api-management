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
package io.gravitee.management.rest.resource;

import static org.mockito.Mockito.mock;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import io.gravitee.management.rest.JerseySpringTest;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.PermissionService;
import io.gravitee.management.service.TeamService;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
public abstract class AbstractResourceTest extends JerseySpringTest {

    @Autowired
    protected ApiService apiService;

    @Autowired
    protected TeamService teamService;

    @Autowired
    protected ApplicationService applicationService;

    @Autowired
    protected PermissionService permissionService;

    @Configuration
    @ComponentScan("io.gravitee.management.rest.resource")
    static class ContextConfiguration {

    	@Bean
    	public ApiService apiService() {
    		return mock(ApiService.class);
    	}

        @Bean
        public TeamService teamService() {
            return mock(TeamService.class);
        }

        @Bean
        public ApplicationService applicationService() {
            return mock(ApplicationService.class);
        }

        @Bean
        public PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }
}
