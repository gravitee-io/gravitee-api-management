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
package io.gravitee.rest.api.service.cockpit.command.adapter;

import io.gravitee.apim.core.cockpit.query_service.CockpitAccessService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.model.InstallationType;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.model.accesspoint.AccessPoint;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.hello.HelloCommand;
import io.gravitee.cockpit.api.command.v1.hello.HelloCommandPayload;
import io.gravitee.cockpit.api.command.v1.hello.HelloReply;
import io.gravitee.cockpit.api.command.v1.installation.AdditionalInfoConstants;
import io.gravitee.common.util.Version;
import io.gravitee.exchange.api.command.CommandAdapter;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.rxjava3.core.Single;
import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HelloCommandAdapter implements CommandAdapter<io.gravitee.exchange.api.command.hello.HelloCommand, HelloCommand, HelloReply> {

    private static final String PATH_SUFFIX = "/";

    @Value("${installation.api.url:http://localhost:8083}")
    private String apiURL;

    @Value("${installation.api.proxyPath.management:${http.api.management.entrypoint:${http.api.entrypoint:/}management}}")
    private String managementProxyPath;

    @Value("${cockpit.auth.path:/auth/cockpit?token={token}}")
    private String authPath;

    @Value("${cockpit.trial:false}")
    private boolean cockpitTrial;

    private final Node node;
    private final InstallationService installationService;
    private final InstallationTypeDomainService installationTypeDomainService;
    private final CockpitAccessService cockpitAccessService;
    private final PluginRegistry pluginRegistry;
    private String buildAuthPath;

    @PostConstruct
    public void afterPropertiesSet() {
        StringBuilder authPathBuilder = new StringBuilder(managementProxyPath);
        if (managementProxyPath.endsWith(PATH_SUFFIX) && authPath.startsWith(PATH_SUFFIX)) {
            authPathBuilder.append(authPath.substring(1));
        } else if (
            (managementProxyPath.endsWith(PATH_SUFFIX) && !authPath.startsWith(PATH_SUFFIX)) ||
            (!managementProxyPath.endsWith(PATH_SUFFIX) && authPath.startsWith(PATH_SUFFIX))
        ) {
            authPathBuilder.append(authPath);
        } else if (!managementProxyPath.endsWith(PATH_SUFFIX) && !authPath.startsWith(PATH_SUFFIX)) {
            authPathBuilder.append(managementProxyPath).append(PATH_SUFFIX).append(authPath);
        }
        this.buildAuthPath = authPathBuilder.toString();
    }

    @Override
    public String supportType() {
        return CockpitCommandType.HELLO.name();
    }

    @Override
    public Single<HelloCommand> adapt(final io.gravitee.exchange.api.command.hello.HelloCommand command) {
        return Single
            .fromCallable(installationService::getOrInitialize)
            .map(installation -> {
                InstallationType installationType = installationTypeDomainService.get();

                HelloCommandPayload.HelloCommandPayloadBuilder<?, ?> payloadBuilder = HelloCommandPayload
                    .builder()
                    .node(
                        io.gravitee.cockpit.api.command.model.Node
                            .builder()
                            .application(node.application())
                            .installationId(installation.getId())
                            .hostname(node.hostname())
                            .version(Version.RUNTIME_VERSION.MAJOR_VERSION)
                            .connectorVersion(connectorVersion())
                            .build()
                    )
                    .installationType(installationType.getLabel())
                    .trial(cockpitTrial)
                    .defaultOrganizationId(GraviteeContext.getDefaultOrganization())
                    .defaultEnvironmentId(GraviteeContext.getDefaultEnvironment());
                Map<String, String> additionalInformation = new HashMap<>(installation.getAdditionalInformation());
                additionalInformation.put(AdditionalInfoConstants.AUTH_PATH, buildAuthPath);
                if (installationType == InstallationType.MULTI_TENANT) {
                    Map<AccessPoint.Type, List<AccessPoint>> accessPointTemplates = new EnumMap<>(AccessPoint.Type.class);
                    cockpitAccessService
                        .getAccessPointsTemplate()
                        .forEach((type, accessPoints) ->
                            accessPointTemplates.put(
                                AccessPoint.Type.valueOf(type.name()),
                                accessPoints
                                    .stream()
                                    .map(accessPoint ->
                                        AccessPoint
                                            .builder()
                                            .host(accessPoint.getHost())
                                            .secured(accessPoint.isSecured())
                                            .target(AccessPoint.Target.valueOf(accessPoint.getTarget().name()))
                                            .build()
                                    )
                                    .toList()
                            )
                        );
                    payloadBuilder.accessPointsTemplate(accessPointTemplates);
                } else {
                    additionalInformation.put(AdditionalInfoConstants.AUTH_BASE_URL, apiURL);
                }
                payloadBuilder.additionalInformation(additionalInformation);
                return new HelloCommand(payloadBuilder.build());
            });
    }

    private String connectorVersion() {
        try {
            return this.pluginRegistry.get("cockpit", "cockpit-connectors-ws").manifest().version();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
