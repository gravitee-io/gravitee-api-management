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

import io.gravitee.apim.core.newtai.use_case.GenerateELUseCase;
import io.gravitee.apim.core.newtai.use_case.SubmitFeedbackUseCase;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.ExpressionLanguageFeedback;
import io.gravitee.rest.api.management.v2.rest.model.FeedbackRequestId;
import io.gravitee.rest.api.management.v2.rest.model.GenerateExpressionLanguage;
import io.gravitee.rest.api.management.v2.rest.model.GenerateExpressionLanguageResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class EnvironmentNewtAIResource extends AbstractResource {

    private static final int TOO_MANY_REQUESTS = 429;
    private static final String RATE_LIMIT_TECHNICAL_CODE = "newtAI.rateLimitExceeded";
    private static final String RATE_LIMIT_MESSAGE = "Newt AI rate limit exceeded";

    @Inject
    private NewtAIRateLimiter rateLimiter;

    @Inject
    private GenerateELUseCase generateELUseCase;

    @Inject
    private SubmitFeedbackUseCase submitFeedbackUseCase;

    @Path("el/_generate")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(HttpStatus.OK)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public Response generate(@Valid GenerateExpressionLanguage generateExpressionLanguage) {
        var decision = rateLimiter.tryAcquire(getAuthenticatedUser());
        if (!decision.allowed()) {
            return rateLimitExceeded(decision.retryAfterSeconds());
        }

        var in = new GenerateELUseCase.Input(generateExpressionLanguage.getMessage(), getAuditInfo());
        if (!StringUtils.isEmpty(generateExpressionLanguage.getContext().getApiId())) {
            in = in.withApiId(generateExpressionLanguage.getContext().getApiId());
        }

        var body = generateELUseCase
            .execute(in)
            .map(result ->
                new GenerateExpressionLanguageResponse()
                    .message(result.message())
                    .feedbackRequestId(
                        new FeedbackRequestId()
                            .chatId(result.feedbackId().chatId())
                            .agentMessageId(result.feedbackId().agentMessageId())
                            .userMessageId(result.feedbackId().userMessageId())
                    )
            )
            .blockingGet();
        return Response.ok(body).build();
    }

    @Path("el/feedback")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public Response feedback(@Valid ExpressionLanguageFeedback expressionLanguageFeedback) {
        // Feedback is not rate-limited so users can still rate answers after hitting the generate cap.
        var in = new SubmitFeedbackUseCase.Input(
            expressionLanguageFeedback.getFeedbackRequestId().getChatId(),
            expressionLanguageFeedback.getFeedbackRequestId().getUserMessageId(),
            expressionLanguageFeedback.getFeedbackRequestId().getAgentMessageId(),
            expressionLanguageFeedback.getAnswerHelpful()
        );
        submitFeedbackUseCase.execute(in).blockingAwait();
        return Response.noContent().build();
    }

    private static Response rateLimitExceeded(long retryAfterSeconds) {
        return Response.status(TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds))
            .entity(new Error().httpStatus(TOO_MANY_REQUESTS).message(RATE_LIMIT_MESSAGE).technicalCode(RATE_LIMIT_TECHNICAL_CODE))
            .build();
    }
}
