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
package io.gravitee.apim.infra.spring;

import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.api.domain_service.ApiDefinitionParserDomainService;
import io.gravitee.apim.core.api.domain_service.ApiHostValidatorDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreServiceSpringConfiguration {

    @Bean
    public VerifyApiPathDomainService verifyApiPathDomainService(
        ApiQueryService apiSearchService,
        AccessPointQueryService accessPointService,
        ApiDefinitionParserDomainService apiDefinitionParserDomainService,
        ApiHostValidatorDomainService apiHostValidatorDomainService
    ) {
        return new VerifyApiPathDomainService(
            apiSearchService,
            accessPointService,
            apiDefinitionParserDomainService,
            apiHostValidatorDomainService
        );
    }

    @Bean
    public ApiDocumentationDomainService apiDocumentationDomainService(PageQueryService pageQueryService) {
        return new ApiDocumentationDomainService(pageQueryService);
    }
}
