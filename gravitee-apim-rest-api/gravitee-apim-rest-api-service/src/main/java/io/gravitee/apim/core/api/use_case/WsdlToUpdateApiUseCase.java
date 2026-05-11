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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.WsdlParserDomainService;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class WsdlToUpdateApiUseCase {

    public sealed interface Input permits Input.Inline, Input.Url {
        String apiId();
        boolean withDocumentation();
        boolean withOASValidationPolicy();
        List<String> withPolicies();
        AuditInfo auditInfo();

        record Inline(
            String apiId,
            String payload,
            boolean withDocumentation,
            boolean withOASValidationPolicy,
            List<String> withPolicies,
            AuditInfo auditInfo
        ) implements Input {}

        record Url(
            String apiId,
            String url,
            boolean withDocumentation,
            boolean withOASValidationPolicy,
            List<String> withPolicies,
            AuditInfo auditInfo
        ) implements Input {}

        static Input of(
            String apiId,
            String payload,
            boolean isUrl,
            boolean withDocumentation,
            boolean withOASValidationPolicy,
            List<String> withPolicies,
            AuditInfo auditInfo
        ) {
            return isUrl
                ? new Url(apiId, payload, withDocumentation, withOASValidationPolicy, withPolicies, auditInfo)
                : new Inline(apiId, payload, withDocumentation, withOASValidationPolicy, withPolicies, auditInfo);
        }
    }

    public record Output(ApiWithFlows apiWithFlows) {}

    private final WsdlParserDomainService wsdlParserDomainService;
    private final OAIToUpdateApiUseCase oaiToUpdateApiUseCase;

    public Output execute(Input input) {
        String content = switch (input) {
            case Input.Url u -> u.url();
            case Input.Inline i -> i.payload();
        };
        String openApiYaml = wsdlParserDomainService.toOpenApiYaml(content);

        var policies = addDependentPolicies(input.withPolicies());

        var importSwaggerDescriptor = ImportSwaggerDescriptorEntity.builder()
            .payload(openApiYaml)
            .format(ImportSwaggerDescriptorEntity.Format.WSDL)
            .withDocumentation(input.withDocumentation())
            .withPolicies(policies)
            .skipFlows(policies != null && policies.isEmpty())
            .build();

        var output = oaiToUpdateApiUseCase.execute(
            OAIToUpdateApiUseCase.Input.builder()
                .apiId(input.apiId())
                .importSwaggerDescriptor(importSwaggerDescriptor)
                .withDocumentation(input.withDocumentation())
                .withOASValidationPolicy(input.withOASValidationPolicy())
                .withPolicyPaths(false)
                .auditInfo(input.auditInfo())
                .build()
        );

        return new Output(output.apiWithFlows());
    }

    private static List<String> addDependentPolicies(List<String> policies) {
        if (policies == null || !policies.contains("rest-to-soap")) {
            return policies;
        }
        return Stream.concat(policies.stream(), Stream.of("xml-json")).distinct().toList();
    }
}
