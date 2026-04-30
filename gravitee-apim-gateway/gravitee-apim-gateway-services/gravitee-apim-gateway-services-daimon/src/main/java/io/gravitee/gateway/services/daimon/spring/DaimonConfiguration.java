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
package io.gravitee.gateway.services.daimon.spring;

import io.gravitee.gateway.services.daimon.DaimonRegistry;
import io.gravitee.gateway.services.daimon.DaimonService;
import io.vertx.ext.web.Router;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DaimonConfiguration {

    @Bean
    public DaimonRegistry daimonRegistry() {
        return new DaimonRegistry();
    }

    @Bean
    public DaimonService daimonService(@Qualifier("managementRouter") Router router, DaimonRegistry registry) {
        return new DaimonService(router, registry);
    }
}
