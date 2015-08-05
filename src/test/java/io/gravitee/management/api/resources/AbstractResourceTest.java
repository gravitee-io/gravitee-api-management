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
package io.gravitee.management.api.resources;

import io.gravitee.management.api.JerseySpringTest;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.api.TeamRepository;

import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
public abstract class AbstractResourceTest extends JerseySpringTest {

    @Configuration
    @ComponentScan("io.gravitee.management.api.resources")
    static class ContextConfiguration {

    	@Bean
    	public TeamRepository teamRepository() {
    		return Mockito.mock(TeamRepository.class);
    	}
    	
        @Bean
        public ApiRepository apiRepository() {
            return Mockito.mock(ApiRepository.class);
        }
    }
}
