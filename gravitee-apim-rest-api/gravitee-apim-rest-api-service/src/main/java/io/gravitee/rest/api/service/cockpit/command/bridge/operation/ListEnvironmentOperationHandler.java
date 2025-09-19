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
package io.gravitee.rest.api.service.cockpit.command.bridge.operation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReplyPayload;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class ListEnvironmentOperationHandler implements BridgeOperationHandler {

    private final EnvironmentService environmentService;
    private final InstallationService installationService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(String bridgeOperation) {
        return Objects.equals(BridgeOperation.LIST_ENVIRONMENT.name(), bridgeOperation);
    }

    @Override
    public Single<BridgeReply> handle(BridgeCommand bridgeCommand) {
        String organizationId = bridgeCommand.getPayload().organizationId();
        try {
            List<BridgeReplyPayload.BridgeReplyContent> replyContents = this.environmentService.findByOrganization(organizationId)
                .stream()
                .map(environmentEntity -> {
                    BridgeReplyPayload.BridgeReplyContent.BridgeReplyContentBuilder builder =
                        BridgeReplyPayload.BridgeReplyContent.builder()
                            .environmentId(environmentEntity.getId())
                            .organizationId(environmentEntity.getOrganizationId())
                            .installationId(installationService.get().getId());
                    try {
                        return builder.content(objectMapper.writeValueAsString(environmentEntity)).build();
                    } catch (JsonProcessingException e) {
                        log.warn("Problem while serializing environment {}", environmentEntity.getId());
                        return builder.error(true).build();
                    }
                })
                .filter(Objects::nonNull)
                .toList();
            return Single.just(new BridgeReply(bridgeCommand.getId(), new BridgeReplyPayload(false, replyContents)));
        } catch (TechnicalManagementException ex) {
            return Single.just(new BridgeReply(bridgeCommand.getId(), "No environment available for organization: " + organizationId));
        }
    }
}
