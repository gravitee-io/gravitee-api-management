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
package io.gravitee.apim.core.newtai.use_case;

import inmemory.InMemoryAlternative;
import inmemory.NewtAIProviderInMemory;
import io.gravitee.apim.core.newtai.exception.NewtAiSubmitFeedbackException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubmitFeedbackUseCaseTest {

    private final NewtAIProviderInMemory newtAIProvider = new NewtAIProviderInMemory();
    private final SubmitFeedbackUseCase submitFeedbackUseCase = new SubmitFeedbackUseCase(newtAIProvider);

    private static final String AGENT_MESSAGE_ID = "agentMessageId";
    private static final String USER_MESSAGE_ID = "userMessageId";
    private static final String CHAT_ID = "chatId";

    @AfterEach
    void tearDown() {
        Stream.of(newtAIProvider).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_submit_feedback_successfully() {
        // Given
        var input = new SubmitFeedbackUseCase.Input(CHAT_ID, USER_MESSAGE_ID, AGENT_MESSAGE_ID, true);

        // When & Then
        submitFeedbackUseCase.execute(input).test().assertComplete();
    }

    @Test
    void should_submit_feedback_with_negative_rating() {
        // Given
        var input = new SubmitFeedbackUseCase.Input(CHAT_ID, USER_MESSAGE_ID, AGENT_MESSAGE_ID, false);

        // When & Then
        submitFeedbackUseCase.execute(input).test().assertComplete();
    }

    @Test
    void should_throw_exception_when_provider_fails() {
        // Given
        var input = new SubmitFeedbackUseCase.Input(CHAT_ID, USER_MESSAGE_ID, AGENT_MESSAGE_ID, true);
        var expectedException = new NewtAiSubmitFeedbackException("commandId", "Unexpected error");
        newtAIProvider.initWith(List.of(new NewtAIProviderInMemory.Tuple.Fail(AGENT_MESSAGE_ID, expectedException)));

        // When & Then
        submitFeedbackUseCase
            .execute(input)
            .test()
            .assertError(throwable ->
                throwable instanceof NewtAiSubmitFeedbackException && throwable.getMessage().equals("Unexpected error")
            );
    }
}
