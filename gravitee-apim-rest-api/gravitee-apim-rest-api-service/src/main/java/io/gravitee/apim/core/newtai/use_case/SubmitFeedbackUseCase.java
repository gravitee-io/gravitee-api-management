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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.newtai.model.ELGenFeedback;
import io.gravitee.apim.core.newtai.service_provider.NewtAIProvider;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class SubmitFeedbackUseCase {

    private final NewtAIProvider newtAIProvider;

    public Completable execute(Input input) {
        return newtAIProvider.submitFeedback(input);
    }

    public record Input(String chatId, String userMessageId, String agentMessageId, boolean answerHelpful) implements ELGenFeedback {}
}
