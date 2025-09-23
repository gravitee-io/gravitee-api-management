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
import io.gravitee.rest.api.management.v2.rest.model.ExpressionLanguageFeedback;
import io.gravitee.rest.api.management.v2.rest.model.FeedbackRequestId;
import io.gravitee.rest.api.management.v2.rest.model.GenerateExpressionLanguage;
import io.gravitee.rest.api.management.v2.rest.model.GenerateExpressionLanguageResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class EnvironmentNewtAIResource extends AbstractResource {

    @Inject
    private GenerateELUseCase generateELUseCase;

    @Inject
    private SubmitFeedbackUseCase submitFeedbackUseCase;

    @Path("el/_generate")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(HttpStatus.OK)
    public GenerateExpressionLanguageResponse generate(@Valid GenerateExpressionLanguage generateExpressionLanguage) {
        var in = new GenerateELUseCase.Input(generateExpressionLanguage.getMessage(), getAuditInfo());
        if (!StringUtils.isEmpty(generateExpressionLanguage.getContext().getApiId())) {
            in = in.withApiId(generateExpressionLanguage.getContext().getApiId());
        }

        return generateELUseCase
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
    }

    @Path("el/feedback")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response feedback(@Valid ExpressionLanguageFeedback expressionLanguageFeedback) {
        var in = new SubmitFeedbackUseCase.Input(
            expressionLanguageFeedback.getFeedbackRequestId().getChatId(),
            expressionLanguageFeedback.getFeedbackRequestId().getUserMessageId(),
            expressionLanguageFeedback.getFeedbackRequestId().getAgentMessageId(),
            expressionLanguageFeedback.getAnswerHelpful()
        );
        submitFeedbackUseCase.execute(in).blockingAwait();
        return Response.noContent().build();
    }
}
