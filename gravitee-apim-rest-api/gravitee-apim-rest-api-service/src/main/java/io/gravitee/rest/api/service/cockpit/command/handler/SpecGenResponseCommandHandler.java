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

package io.gravitee.rest.api.service.cockpit.command.handler;

import static io.gravitee.exchange.api.command.CommandStatus.SUCCEEDED;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.use_case.ApiCreateDocumentationPageUseCase;
import io.gravitee.apim.core.specgen.use_case.BuildSpecGenPageResponseUseCase;
import io.gravitee.apim.core.specgen.use_case.BuildSpecGenPageResponseUseCase.Input;
import io.gravitee.apim.core.specgen.use_case.NotifySpecGenResponseUseCase;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.specgen.response.SpecGenResponseCommand;
import io.gravitee.cockpit.api.command.v1.specgen.response.SpecGenResponseReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SpecGenResponseCommandHandler implements CommandHandler<SpecGenResponseCommand, SpecGenResponseReply> {

    private final BuildSpecGenPageResponseUseCase saveSpecGenResponseUseCase;
    private final ApiCreateDocumentationPageUseCase createDocumentationPageUseCase;
    private final NotifySpecGenResponseUseCase notifySpecGenResponseUseCase;

    public SpecGenResponseCommandHandler(
        @Lazy BuildSpecGenPageResponseUseCase saveSpecGenResponseUseCase,
        @Lazy ApiCreateDocumentationPageUseCase createDocumentationPageUseCase,
        @Lazy NotifySpecGenResponseUseCase NotifySpecGenResponseUseCase
    ) {
        this.saveSpecGenResponseUseCase = saveSpecGenResponseUseCase;
        this.createDocumentationPageUseCase = createDocumentationPageUseCase;
        this.notifySpecGenResponseUseCase = NotifySpecGenResponseUseCase;
    }

    @Override
    public String supportType() {
        return CockpitCommandType.SPEC_GEN_RESPONSE.name();
    }

    @Override
    public Single<SpecGenResponseReply> handle(SpecGenResponseCommand command) {
        var payload = command.getPayload().payload();
        return saveSpecGenResponseUseCase
            .execute(new Input(payload.apiId(), payload.result()))
            .map(page ->
                createDocumentationPageUseCase.execute(new ApiCreateDocumentationPageUseCase.Input(page, getAuditInfo(payload.userId())))
            )
            .doOnError(t ->
                log.error("An error has occurred while trying to save generated specification for api: [{}]", payload.apiId(), t)
            )
            .ignoreElement()
            .andThen(Single.just(new SpecGenResponseReply(command.getId(), SUCCEEDED)))
            .onErrorResumeNext(t -> Single.just(new SpecGenResponseReply(command.getId(), t.getMessage())))
            .doOnSuccess(reply -> {
                if (SUCCEEDED.equals(reply.getCommandStatus())) {
                    notifySpecGenResponseUseCase.notify(payload.apiId(), payload.userId());
                }
            });
    }

    private static AuditInfo getAuditInfo(String userId) {
        var context = getExecutionContext();
        return AuditInfo
            .builder()
            .environmentId(context.getEnvironmentId())
            .organizationId(context.getOrganizationId())
            .actor(AuditActor.builder().userId(userId).build())
            .build();
    }
}
