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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import inmemory.NewtAIProviderInMemory;
import io.gravitee.apim.core.newtai.exception.NewtAiSubmitFeedbackException;
import io.gravitee.apim.core.newtai.model.ELGenReply;
import io.gravitee.rest.api.management.v2.rest.model.GenerateExpressionLanguageResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EnvironmentNewtAIResourceTest extends AbstractResourceTest {

    @Inject
    protected NewtAIProviderInMemory newtAIProvider;

    private static final String ENVIRONMENT = "fake-env";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/newtai";
    }

    @BeforeEach
    void setup() {
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT);
        environment.setOrganizationId(ORGANIZATION);

        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environment);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        newtAIProvider.reset();
        GraviteeContext.cleanContext();
    }

    @Nested
    class GenerateExpressionLanguage {

        @Test
        void should_generate_expression_language() {
            // Given
            String requestJson =
                """
                {
                    "message": "Generate an expression to get the user ID",
                    "context": {
                        "apiId": "api-123"
                    }
                }
                """;

            var expectedReply = new TestELGenReply(
                "Generated expression: {#context.attributes['user-id']}",
                new TestELGenReply.TestFeedbackId("chat-123", "user-msg-456", "agent-msg-789")
            );
            newtAIProvider.initWith(
                List.of(new NewtAIProviderInMemory.Tuple.Success("Generate an expression to get the user ID", expectedReply))
            );

            // When
            final Response response = rootTarget("el/_generate").request().post(Entity.json(requestJson));

            // Then
            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(GenerateExpressionLanguageResponse.class);
            assertThat(body.getMessage()).isEqualTo("Generated expression: {#context.attributes['user-id']}");
            assertThat(body.getFeedbackRequestId()).isNotNull();
            assertThat(body.getFeedbackRequestId().getChatId()).isEqualTo("chat-123");
            assertThat(body.getFeedbackRequestId().getUserMessageId()).isEqualTo("user-msg-456");
            assertThat(body.getFeedbackRequestId().getAgentMessageId()).isEqualTo("agent-msg-789");
        }

        @Test
        void should_generate_expression_language_without_api_context() {
            // Given
            String requestJson =
                """
                {
                    "message": "Generate a simple expression",
                    "context": {}
                }
                """;

            var expectedReply = new TestELGenReply(
                "Simple expression: {#request.timestamp}",
                new TestELGenReply.TestFeedbackId("chat-456", "user-msg-789", "agent-msg-012")
            );
            newtAIProvider.initWith(List.of(new NewtAIProviderInMemory.Tuple.Success("Generate a simple expression", expectedReply)));

            // When
            final Response response = rootTarget("el/_generate").request().post(Entity.json(requestJson));

            // Then
            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(GenerateExpressionLanguageResponse.class);
            assertThat(body.getMessage()).isEqualTo("Simple expression: {#request.timestamp}");
            assertThat(body.getFeedbackRequestId()).isNotNull();
            assertThat(body.getFeedbackRequestId().getChatId()).isEqualTo("chat-456");
        }
    }

    @Nested
    class SubmitFeedback {

        @Test
        void should_submit_positive_feedback() {
            // Given
            String requestJson =
                """
                {
                    "feedbackRequestId": {
                        "chatId": "chat-123",
                        "userMessageId": "user-msg-456",
                        "agentMessageId": "agent-msg-789"
                    },
                    "answerHelpful": true
                }
                """;

            // When
            final Response response = rootTarget("el/feedback").request().post(Entity.json(requestJson));

            // Then
            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);
        }

        @Test
        void should_submit_negative_feedback() {
            // Given
            String requestJson =
                """
                {
                    "feedbackRequestId": {
                        "chatId": "chat-456",
                        "userMessageId": "user-msg-789",
                        "agentMessageId": "agent-msg-012"
                    },
                    "answerHelpful": false
                }
                """;

            // When
            final Response response = rootTarget("el/feedback").request().post(Entity.json(requestJson));

            // Then
            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);
        }

        @Test
        void should_return_500_when_error_occurs() {
            // Given
            String requestJson =
                """
                {
                    "feedbackRequestId": {
                        "chatId": "chat-123",
                        "userMessageId": "user-msg-456",
                        "agentMessageId": "agent-msg-789"
                    },
                    "answerHelpful": true
                }
                """;
            var expectedException = new NewtAiSubmitFeedbackException("commandId", "Unexpected error");
            newtAIProvider.initWith(List.of(new NewtAIProviderInMemory.Tuple.Fail("agent-msg-789", expectedException)));

            // When
            final Response response = rootTarget("el/feedback").request().post(Entity.json(requestJson));

            // Then
            assertThat(response.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR_500);
        }
    }

    private record TestELGenReply(String message, TestFeedbackId feedbackId) implements ELGenReply {
        private record TestFeedbackId(String chatId, String userMessageId, String agentMessageId) implements ELGenReply.FeedbackId {}
    }
}
