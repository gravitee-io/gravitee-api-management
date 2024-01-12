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
package io.gravitee.rest.api.service.cockpit.command.producer;

import io.gravitee.apim.core.cockpit.query_service.CockpitAccessService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.model.InstallationType;
import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandProducer;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.accesspoint.AccessPoint;
import io.gravitee.cockpit.api.command.hello.HelloCommand;
import io.gravitee.cockpit.api.command.hello.HelloReply;
import io.gravitee.cockpit.api.command.installation.AdditionalInfoConstants;
import io.gravitee.node.api.Node;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.rxjava3.core.Single;
import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("cockpitHelloCommandProducer")
@RequiredArgsConstructor
@Slf4j
public class HelloCommandProducer implements CommandProducer<HelloCommand, HelloReply> {

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
    private final EnvironmentService environmentService;
    private final OrganizationService organizationService;
    private final InstallationTypeDomainService installationTypeDomainService;
    private final CockpitAccessService cockpitAccessService;

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
    public Command.Type produceType() {
        return Command.Type.HELLO_COMMAND;
    }

    @Override
    public Single<HelloCommand> prepare(HelloCommand command) {
        final InstallationEntity installation = installationService.getOrInitialize();

        command.getPayload().getNode().setInstallationId(installation.getId());
        command.getPayload().getNode().setHostname(node.hostname());
        command.getPayload().getAdditionalInformation().putAll(installation.getAdditionalInformation());

        InstallationType installationType = installationTypeDomainService.get();
        command.getPayload().setInstallationType(installationType.getLabel());
        command.getPayload().setTrial(cockpitTrial);
        command.getPayload().getAdditionalInformation().put(AdditionalInfoConstants.AUTH_PATH, buildAuthPath);
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
            command.getPayload().setAccessPointsTemplate(accessPointTemplates);
        } else {
            command.getPayload().getAdditionalInformation().put(AdditionalInfoConstants.AUTH_BASE_URL, apiURL);
        }

        command.getPayload().setDefaultOrganizationId(GraviteeContext.getDefaultOrganization());
        command.getPayload().setDefaultEnvironmentId(GraviteeContext.getDefaultEnvironment());

        return Single.just(command);
    }

    @Override
    public Single<HelloReply> handleReply(HelloReply reply) {
        if (reply.getCommandStatus() == CommandStatus.SUCCEEDED) {
            final Map<String, String> additionalInformation = installationService.getOrInitialize().getAdditionalInformation();
            additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_ID, reply.getInstallationId());
            additionalInformation.put(InstallationService.COCKPIT_INSTALLATION_STATUS, reply.getInstallationStatus());
            installationService.setAdditionalInformation(additionalInformation);

            if (reply.getDefaultEnvironmentCockpitId() != null) {
                updateDefaultEnvironmentCockpitId(reply.getDefaultEnvironmentCockpitId());
            }

            if (reply.getDefaultOrganizationCockpitId() != null) {
                updateDefaultOrganizationCockpitId(reply.getDefaultOrganizationCockpitId());
            }
        }

        return Single.just(reply);
    }

    private void updateDefaultEnvironmentCockpitId(String defaultEnvironmentCockpitId) {
        EnvironmentEntity defaultEnvironment = environmentService.getDefaultOrInitialize();

        UpdateEnvironmentEntity updateEnvironment = new UpdateEnvironmentEntity(defaultEnvironment);
        updateEnvironment.setCockpitId(defaultEnvironmentCockpitId);

        environmentService.createOrUpdate(defaultEnvironment.getOrganizationId(), defaultEnvironment.getId(), updateEnvironment);
    }

    private void updateDefaultOrganizationCockpitId(String defaultOrganizationCockpitId) {
        OrganizationEntity defaultOrganization = organizationService.getDefaultOrInitialize();

        UpdateOrganizationEntity updateOrganization = new UpdateOrganizationEntity(defaultOrganization);
        updateOrganization.setCockpitId(defaultOrganizationCockpitId);

        organizationService.updateOrganization(defaultOrganization.getId(), updateOrganization);
    }
}
