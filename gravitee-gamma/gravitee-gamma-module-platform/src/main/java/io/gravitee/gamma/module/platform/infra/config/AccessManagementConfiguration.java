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
package io.gravitee.gamma.module.platform.infra.config;

import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import io.gravitee.apim.plugin.gamma.api.identity.ApimAmConnectionRepository;
import io.gravitee.gamma.module.platform.core.am.port.service_provider.AmConnectionTester;
import io.gravitee.gamma.module.platform.core.am.port.service_provider.AmDirectoryClient;
import io.gravitee.gamma.module.platform.core.am.use_case.GetDomainUseCase;
import io.gravitee.gamma.module.platform.core.am.use_case.ListDomainEntrypointsUseCase;
import io.gravitee.gamma.module.platform.core.am.use_case.ListDomainsUseCase;
import io.gravitee.gamma.module.platform.core.am.use_case.ListEnvironmentsUseCase;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.DeleteAmConnectionUseCase;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.GetAmConnectionUseCase;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.SaveAmConnectionUseCase;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.TestAmConnectionUseCase;
import io.gravitee.gamma.module.platform.infra.service_provider.AmSdkClientFactory;
import io.gravitee.gamma.module.platform.infra.service_provider.AmSdkConnectionTester;
import io.gravitee.gamma.module.platform.infra.service_provider.AmSdkDirectoryClient;
import io.gravitee.rest.api.service.AmConnectionService;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessManagementConfiguration {

    // AM connection storage + token encryption live in APIM rest-api core (AmConnectionService),
    // injected from the plugin's parent Spring context. The module delegates through this adapter.
    // Named + qualified so it doesn't clash with the AIM / authorization gamma modules'
    // AmConnectionRepository beans, which share the same parent context (each gamma plugin context
    // inherits the parent, so a by-type lookup would otherwise be ambiguous).
    @Bean
    public AmConnectionRepository platformAmConnectionRepository(AmConnectionService amConnectionService) {
        return new ApimAmConnectionRepository(amConnectionService);
    }

    @Bean
    public AmSdkClientFactory amSdkClientFactory(
        Vertx vertx,
        @Qualifier("platformAmConnectionRepository") AmConnectionRepository amConnectionRepository
    ) {
        return new AmSdkClientFactory(vertx, amConnectionRepository);
    }

    @Bean
    public AmDirectoryClient amDirectoryClient(AmSdkClientFactory amSdkClientFactory) {
        return new AmSdkDirectoryClient(amSdkClientFactory);
    }

    @Bean
    public AmConnectionTester amConnectionTester(AmSdkClientFactory amSdkClientFactory) {
        return new AmSdkConnectionTester(amSdkClientFactory);
    }

    @Bean
    public GetAmConnectionUseCase getAmConnectionUseCase(
        @Qualifier("platformAmConnectionRepository") AmConnectionRepository amConnectionRepository
    ) {
        return new GetAmConnectionUseCase(amConnectionRepository);
    }

    @Bean
    public SaveAmConnectionUseCase saveAmConnectionUseCase(
        @Qualifier("platformAmConnectionRepository") AmConnectionRepository amConnectionRepository
    ) {
        return new SaveAmConnectionUseCase(amConnectionRepository);
    }

    @Bean
    public DeleteAmConnectionUseCase deleteAmConnectionUseCase(
        @Qualifier("platformAmConnectionRepository") AmConnectionRepository amConnectionRepository
    ) {
        return new DeleteAmConnectionUseCase(amConnectionRepository);
    }

    @Bean
    public TestAmConnectionUseCase testAmConnectionUseCase(
        @Qualifier("platformAmConnectionRepository") AmConnectionRepository amConnectionRepository,
        AmConnectionTester amConnectionTester
    ) {
        return new TestAmConnectionUseCase(amConnectionRepository, amConnectionTester);
    }

    @Bean
    public ListEnvironmentsUseCase listEnvironmentsUseCase(AmDirectoryClient amDirectoryClient) {
        return new ListEnvironmentsUseCase(amDirectoryClient);
    }

    @Bean
    public ListDomainsUseCase listDomainsUseCase(AmDirectoryClient amDirectoryClient) {
        return new ListDomainsUseCase(amDirectoryClient);
    }

    @Bean
    public GetDomainUseCase getDomainUseCase(AmDirectoryClient amDirectoryClient) {
        return new GetDomainUseCase(amDirectoryClient);
    }

    @Bean
    public ListDomainEntrypointsUseCase listDomainEntrypointsUseCase(AmDirectoryClient amDirectoryClient) {
        return new ListDomainEntrypointsUseCase(amDirectoryClient);
    }
}
