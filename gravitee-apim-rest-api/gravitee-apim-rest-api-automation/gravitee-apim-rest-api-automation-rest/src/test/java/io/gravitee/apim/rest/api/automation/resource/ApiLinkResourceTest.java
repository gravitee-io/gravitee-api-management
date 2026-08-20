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
package io.gravitee.apim.rest.api.automation.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.exception.PortalLinkNotFoundException;
import io.gravitee.apim.core.portal_page.use_case.CreateOrUpdateApiLinkUseCase;
import io.gravitee.apim.core.portal_page.use_case.DeleteApiLinkUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetApiLinkUseCase;
import io.gravitee.apim.core.portal_page.use_case.ValidateApiLinkUseCase;
import io.gravitee.apim.rest.api.automation.model.PortalLinkState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiLinkResourceTest extends AbstractResourceTest {

    private static final String API_HRID = "my-api";
    private static final String LINK_HRID = "external-docs";

    @Inject
    private CreateOrUpdateApiLinkUseCase createOrUpdateApiLinkUseCase;

    @Inject
    private ValidateApiLinkUseCase validateApiLinkUseCase;

    @Inject
    private GetApiLinkUseCase getApiLinkUseCase;

    @Inject
    private DeleteApiLinkUseCase deleteApiLinkUseCase;

    @AfterEach
    void tearDown() {
        reset(createOrUpdateApiLinkUseCase, validateApiLinkUseCase, getApiLinkUseCase, deleteApiLinkUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis/" + API_HRID + "/links";
    }

    @Nested
    class Run {

        @Test
        void should_apply_and_return_the_api_link_state() {
            when(createOrUpdateApiLinkUseCase.execute(any())).thenReturn(new CreateOrUpdateApiLinkUseCase.Output(null, List.of()));

            try (
                var response = rootTarget().request().accept(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(readJSON("api-link.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(createOrUpdateApiLinkUseCase).execute(any(CreateOrUpdateApiLinkUseCase.Input.class));

                var state = response.readEntity(PortalLinkState.class);
                assertThat(state.getApiHrid()).isEqualTo(API_HRID);
                assertThat(state.getPortalHrid()).isNull();
            }
        }
    }

    @Nested
    class Get {

        @Test
        void should_return_404_when_the_link_does_not_exist() {
            when(getApiLinkUseCase.execute(any())).thenThrow(new PortalLinkNotFoundException(LINK_HRID));

            try (var response = rootTarget(LINK_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }
}
