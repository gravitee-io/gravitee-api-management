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

import static io.gravitee.apim.core.documentation.model.Page.ReferenceType.API;
import static io.gravitee.apim.core.documentation.model.Page.Visibility.PRIVATE;
import static io.gravitee.apim.core.specgen.use_case.BuildSpecGenPageResponseUseCase.METADATA;
import static io.gravitee.cockpit.api.command.v1.CockpitCommandType.SPEC_GEN_RESPONSE;
import static io.gravitee.exchange.api.command.CommandStatus.ERROR;
import static io.gravitee.exchange.api.command.CommandStatus.SUCCEEDED;
import static io.gravitee.rest.api.service.common.UuidString.generateRandom;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.Page.Type;
import io.gravitee.apim.core.documentation.use_case.ApiCreateDocumentationPageUseCase;
import io.gravitee.apim.core.documentation.use_case.ApiCreateDocumentationPageUseCase.Output;
import io.gravitee.apim.core.specgen.use_case.BuildSpecGenPageResponseUseCase;
import io.gravitee.apim.core.specgen.use_case.BuildSpecGenPageResponseUseCase.Input;
import io.gravitee.apim.core.specgen.use_case.NotifySpecGenResponseUseCase;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.specgen.SpecGenCommandPayload;
import io.gravitee.cockpit.api.command.v1.specgen.response.SpecGenResponseCommand;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.spec.gen.api.SpecGenResponse;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SpecGenResponseCommandHandlerTest {

    @Mock
    BuildSpecGenPageResponseUseCase buildSpecGenPageResponseUseCase;

    @Mock
    ApiCreateDocumentationPageUseCase createDocumentationPageUseCase;

    @Mock
    NotifySpecGenResponseUseCase portalNotificationService;

    SpecGenResponseCommandHandler specGenResponseCommandHandler;

    @BeforeEach
    void setUp() {
        specGenResponseCommandHandler =
            new SpecGenResponseCommandHandler(buildSpecGenPageResponseUseCase, createDocumentationPageUseCase, portalNotificationService);
    }

    @Test
    void must_not_save_page_due_to_build_page_error() {
        final String errorMessage = "An error has occurred";
        when(buildSpecGenPageResponseUseCase.execute(any())).thenReturn(Maybe.error(new IllegalArgumentException(errorMessage)));

        assertThat(specGenResponseCommandHandler.supportType()).isEqualTo(SPEC_GEN_RESPONSE.name());
        specGenResponseCommandHandler
            .handle(new SpecGenResponseCommand(generatePayload()))
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> ERROR.equals(reply.getCommandStatus()))
            .assertValue(reply -> errorMessage.equals(reply.getErrorDetails()));
    }

    @NotNull
    private static SpecGenCommandPayload<SpecGenResponse> generatePayload() {
        return new SpecGenCommandPayload<>(
            generateRandom(),
            generateRandom(),
            generateRandom(),
            generateRandom(),
            new SpecGenResponse(generateRandom(), "openapi: 3.0.3", generateRandom())
        );
    }

    @Test
    void must_not_save_page_due_to_create_documentation_page_error() {
        final String errorMessage = "An error has occurred";
        when(buildSpecGenPageResponseUseCase.execute(any())).thenReturn(Maybe.just(getPage()));

        var payload = generatePayload();

        GraviteeContext.setCurrentEnvironment(payload.environmentId());
        GraviteeContext.setCurrentOrganization(payload.organizationId());

        when(createDocumentationPageUseCase.execute(any())).thenThrow(new IllegalArgumentException(errorMessage));

        assertThat(specGenResponseCommandHandler.supportType()).isEqualTo(SPEC_GEN_RESPONSE.name());
        specGenResponseCommandHandler
            .handle(new SpecGenResponseCommand(payload))
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> ERROR.equals(reply.getCommandStatus()))
            .assertValue(reply -> errorMessage.equals(reply.getErrorDetails()));
    }

    @Test
    void must_save_page_due_to_create_documentation_page() {
        final Page page = getPage();
        when(buildSpecGenPageResponseUseCase.execute(any())).thenReturn(Maybe.just(page));

        var payload = generatePayload();

        GraviteeContext.setCurrentEnvironment(payload.environmentId());
        GraviteeContext.setCurrentOrganization(payload.organizationId());

        when(createDocumentationPageUseCase.execute(any())).thenReturn(new Output(page));

        assertThat(specGenResponseCommandHandler.supportType()).isEqualTo(SPEC_GEN_RESPONSE.name());
        specGenResponseCommandHandler
            .handle(new SpecGenResponseCommand(payload))
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> SUCCEEDED.equals(reply.getCommandStatus()));
    }

    private static Page getPage() {
        return Page
            .builder()
            .referenceId(generateRandom())
            .referenceType(API)
            .name("Api Name")
            .content("openapi: 3.0.3")
            .type(Type.SWAGGER)
            .visibility(PRIVATE)
            .homepage(false)
            .hidden(true)
            .published(false)
            .metadata(METADATA)
            .build();
    }

    @AfterEach
    void after() {
        GraviteeContext.cleanContext();
    }
}
