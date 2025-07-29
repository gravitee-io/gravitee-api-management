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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import inmemory.InMemoryAlternative;
import inmemory.NewtAIProviderInMemory;
import io.gravitee.apim.core.apim.service_provider.ApimProductInfo;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.newtai.exception.NewtAIReplyException;
import io.gravitee.apim.core.newtai.model.ELGenReply;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.stream.Stream;
import lombok.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(VertxExtension.class)
class GenerateELUseCaseTest {

    private final NewtAIProviderInMemory newtAIProvider = new NewtAIProviderInMemory();
    private final ProductInfo apimMetadata = new ProductInfo();
    private final GenerateELUseCase generateELUseCase = new GenerateELUseCase(newtAIProvider, apimMetadata);

    @BeforeEach
    void setUp() {
        apimMetadata.setVersion("4.9");
    }

    @AfterEach
    void tearDown() {
        Stream.of(newtAIProvider).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_generate_el() {
        // Given
        var input = new GenerateELUseCase.Input("apiId", "message", mock(AuditInfo.class));
        var expectedReply = new Reply("el-expression", new Reply.FeedbackId("chatId", "userMessageId", "agentMessageId"));
        newtAIProvider.initWith(List.of(new NewtAIProviderInMemory.Tuple.Success("message", expectedReply)));

        // When
        var reply = generateELUseCase.execute(input).blockingGet();

        // Then
        assertThat(reply.message()).isEqualTo("el-expression");
        assertThat(reply.feedbackId().chatId()).isEqualTo("chatId");
        assertThat(reply.feedbackId().userMessageId()).isEqualTo("userMessageId");
        assertThat(reply.feedbackId().agentMessageId()).isEqualTo("agentMessageId");
    }

    @Test
    void fail_generate_el(VertxTestContext context) {
        // Given
        var input = new GenerateELUseCase.Input("apiId", "message", mock(AuditInfo.class));
        newtAIProvider.initWith(List.of(new NewtAIProviderInMemory.Tuple.Fail("message", new NewtAIReplyException("cmdId", "error"))));

        // When
        var genReplySingle = generateELUseCase.execute(input);

        // Then
        genReplySingle.subscribe(
            reply -> context.failNow("Should not succeed"),
            th ->
                context.verify(() -> {
                    assertThat(th).isInstanceOf(NewtAIReplyException.class).hasMessage("error");
                    context.completeNow();
                })
        );
    }

    private record Reply(String message, FeedbackId feedbackId) implements ELGenReply {
        private record FeedbackId(String chatId, String userMessageId, String agentMessageId) implements ELGenReply.FeedbackId {}
    }

    @Data
    private static class ProductInfo implements ApimProductInfo {

        private String name;
        private String version;
    }
}
