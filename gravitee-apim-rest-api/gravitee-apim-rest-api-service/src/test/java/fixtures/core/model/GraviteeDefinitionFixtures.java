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
package fixtures.core.model;

import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.api.model.import_definition.ApiMemberRole;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PageExport;
import io.gravitee.apim.core.api.model.import_definition.PlanExport;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingContent;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.logging.LoggingPhase;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancer;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class GraviteeDefinitionFixtures {

    private GraviteeDefinitionFixtures() {}

    public static final Supplier<GraviteeDefinition.GraviteeDefinitionBuilder> BASE = () ->
        GraviteeDefinition
            .builder()
            .members(
                Set.of(
                    ApiMember
                        .builder()
                        .displayName("John Doe")
                        .id("member-id")
                        .roles(List.of(ApiMemberRole.builder().name("PRIMARY_OWNER").scope(RoleScope.API).build()))
                        .build()
                )
            )
            .metadata(
                Set.of(
                    NewApiMetadata
                        .builder()
                        .format(Metadata.MetadataFormat.MAIL)
                        .key("email-support")
                        .name("email-support")
                        .value("${(api.primaryOwner.email)!''}")
                        .build()
                )
            )
            .apiMedia(List.of(MediaFixtures.aMedia()))
            .apiPicture("data:image/png;base64,picture")
            .apiBackground("data:image/png;base64,background");

    public static GraviteeDefinition aGraviteeDefinitionProxy() {
        return BASE
            .get()
            .api(
                ApiExport
                    .builder()
                    .definitionVersion(DefinitionVersion.V4)
                    .type(ApiType.PROXY)
                    .listeners(
                        List.of(
                            HttpListener
                                .builder()
                                .paths(List.of(new Path(null, "/proxy-api", true)))
                                .entrypoints(List.of(Entrypoint.builder().type("http-proxy").qos(Qos.AUTO).configuration("{}").build()))
                                .build()
                        )
                    )
                    .endpointGroups(
                        List.of(
                            EndpointGroup
                                .builder()
                                .name("Default HTTP proxy group")
                                .type("http-proxy")
                                .loadBalancer(LoadBalancer.builder().type(LoadBalancerType.ROUND_ROBIN).build())
                                .sharedConfiguration(
                                    """
                                                {
                                                  "proxy" : {
                                                    "useSystemProxy" : false,
                                                    "enabled" : false
                                                  },
                                                  "http" : {
                                                    "keepAliveTimeout" : 30000,
                                                    "keepAlive" : true,
                                                    "followRedirects" : false,
                                                    "readTimeout" : 10000,
                                                    "idleTimeout" : 60000,
                                                    "connectTimeout" : 3000,
                                                    "useCompression" : true,
                                                    "maxConcurrentConnections" : 20,
                                                    "version" : "HTTP_1_1",
                                                    "pipelining" : false
                                                  },
                                                  "ssl" : {
                                                    "keyStore" : {
                                                      "type" : ""
                                                    },
                                                    "hostnameVerifier" : true,
                                                    "trustStore" : {
                                                      "type" : ""
                                                    },
                                                    "trustAll" : false
                                                 }
                                                }"""
                                )
                                .endpoints(
                                    List.of(
                                        Endpoint
                                            .builder()
                                            .name("Default HTTP proxy")
                                            .type("http-proxy")
                                            .inheritConfiguration(true)
                                            .weight(1)
                                            .configuration(
                                                """
                                                                            {"target":"https://api.gravitee.io/echo"}"""
                                            )
                                            .sharedConfigurationOverride("{}")
                                            .services(new EndpointServices())
                                            .build()
                                    )
                                )
                                .services(
                                    EndpointGroupServices
                                        .builder()
                                        .healthCheck(
                                            Service
                                                .builder()
                                                .type("http-health-check")
                                                .enabled(true)
                                                .configuration(
                                                    """
                                                                                {
                                                                                  "schedule" : "*/1 * * * * *",
                                                                                  "headers" : [ ],
                                                                                  "overrideEndpointPath" : true,
                                                                                  "method" : "GET",
                                                                                  "failureThreshold" : 2,
                                                                                  "assertion" : "{#response.status == 200}",
                                                                                  "successThreshold" : 2,
                                                                                  "target" : "/"
                                                                                }"""
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                    )
                    .analytics(
                        Analytics
                            .builder()
                            .enabled(true)
                            .logging(
                                Logging
                                    .builder()
                                    .condition("{#request.timestamp <= 1709737299215l}")
                                    .content(LoggingContent.builder().headers(true).payload(true).build())
                                    .phase(LoggingPhase.builder().request(true).response(true).build())
                                    .mode(LoggingMode.builder().entrypoint(true).endpoint(true).build())
                                    .build()
                            )
                            .build()
                    )
                    .flowExecution(new FlowExecution())
                    .flows(
                        List.of(
                            Flow
                                .builder()
                                .id("flow-id")
                                .name("api flows")
                                .enabled(true)
                                .selectors(
                                    List.of(HttpSelector.builder().path("/").pathOperator(Operator.STARTS_WITH).methods(Set.of()).build())
                                )
                                .request(List.of())
                                .response(List.of())
                                .subscribe(List.of())
                                .publish(List.of())
                                .tags(Set.of())
                                .build()
                        )
                    )
                    .id("api-id")
                    .name("My Api")
                    .description("My Api description")
                    .apiVersion("1.0.0")
                    .createdAt(Instant.parse("2023-11-07T15:17:44.946Z"))
                    .deployedAt(Instant.parse("2024-11-08T10:22:17.487Z"))
                    .updatedAt(Instant.parse("2024-11-13T14:31:05.066Z"))
                    .groups(Set.of("group1"))
                    .state(Lifecycle.State.STARTED)
                    .visibility(Visibility.PUBLIC)
                    .lifecycleState(ApiLifecycleState.PUBLISHED)
                    .tags(Set.of())
                    .categories(Set.of())
                    .originContext(new OriginContext.Management())
                    .responseTemplates(Map.of("DEFAULT", Map.of("*.*", ResponseTemplate.builder().statusCode(200).build())))
                    .properties(List.of(Property.builder().key("prop-key").value("prop-value").build()))
                    .resources(List.of())
                    .build()
            )
            .plans(
                Set.of(
                    PlanExport
                        .builder()
                        .id("plan-id")
                        .name("Default Keyless (UNSECURED)")
                        .definitionVersion(DefinitionVersion.V4)
                        .description("Default unsecured plan")
                        .createdAt(Instant.parse("2023-11-07T15:17:46.156Z"))
                        .publishedAt(Instant.parse("2023-11-07T15:17:46.295Z"))
                        .updatedAt(Instant.parse("2023-12-05T07:33:32.922Z"))
                        .type(Plan.PlanType.API)
                        .mode(PlanMode.STANDARD)
                        .security(PlanSecurity.builder().type("KEY_LESS").configuration("{}").build())
                        .status(PlanStatus.PUBLISHED)
                        .apiId("api-id")
                        .order(1)
                        .commentRequired(false)
                        .flows(List.of())
                        .validation(Plan.PlanValidationType.AUTO)
                        .type(Plan.PlanType.API)
                        .build()
                )
            )
            .pages(
                List.of(
                    PageExport
                        .builder()
                        .id("page-id")
                        .referenceType(Page.ReferenceType.API)
                        .referenceId("api-id")
                        .name("openapi.json")
                        .type(Page.Type.SWAGGER)
                        .content(
                            "{\n  \"openapi\" : \"3.0.1\",\n  \"info\" : {\n    \"title\" : \"Echo\",\n    \"description\" : \"This is an echo API \",\n    \"version\" : \"2024-06-25T06:52:05Z\"\n  },\n  \"servers\" : [ {\n    \"url\" : \"https://5a1pbut3t9.execute-api.eu-west-3.amazonaws.com/{basePath}\",\n    \"variables\" : {\n      \"basePath\" : {\n        \"default\" : \"dev\"\n      }\n    }\n  } ],\n  \"paths\" : {\n    \"/echo\" : {\n      \"get\" : {\n        \"operationId\": \"echo\",\n        \"responses\" : {\n          \"200\" : {\n            \"description\" : \"200 response\",\n            \"headers\" : {\n              \"Access-Control-Allow-Origin\" : {\n                \"schema\" : {\n                  \"type\" : \"string\"\n                }\n              }\n            },\n            \"content\" : {\n              \"application/json\" : {\n                \"schema\" : {\n                  \"$ref\" : \"#/components/schemas/Empty\"\n                }\n              }\n            }\n          }\n        },\n        \"security\" : [ {\n          \"api_key\" : [ ]\n        } ]\n      },\n      \"options\" : {\n        \"responses\" : {\n          \"200\" : {\n            \"description\" : \"200 response\",\n            \"headers\" : {\n              \"Access-Control-Allow-Origin\" : {\n                \"schema\" : {\n                  \"type\" : \"string\"\n                }\n              },\n              \"Access-Control-Allow-Methods\" : {\n                \"schema\" : {\n                  \"type\" : \"string\"\n                }\n              },\n              \"Access-Control-Allow-Headers\" : {\n                \"schema\" : {\n                  \"type\" : \"string\"\n                }\n              }\n            },\n            \"content\" : {\n              \"application/json\" : {\n                \"schema\" : {\n                  \"$ref\" : \"#/components/schemas/Empty\"\n                }\n              }\n            }\n          }\n        }\n      }\n    }\n  },\n  \"components\" : {\n    \"schemas\" : {\n      \"Empty\" : {\n        \"title\" : \"Empty Schema\",\n        \"type\" : \"object\"\n      }\n    },\n    \"securitySchemes\" : {\n      \"api_key\" : {\n        \"type\" : \"apiKey\",\n        \"name\" : \"x-api-key\",\n        \"in\" : \"header\"\n      }\n    }\n  }\n}"
                        )
                        .published(true)
                        .visibility(Page.Visibility.PUBLIC)
                        .order(0)
                        .createdAt(Instant.parse("2024-09-13T07:00:07.874Z"))
                        .updatedAt(Instant.parse("2024-09-13T08:00:07.874Z"))
                        .accessControls(Set.of())
                        .referenceId("api-id")
                        .referenceType(Page.ReferenceType.API)
                        .build()
                )
            )
            .build();
    }
}
