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
package io.gravitee.gateway.reactive.core.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ProcessorChainTest {

    @Mock
    private Processor mockProcessor;

    @Test
    public void shouldCompleteWithNullProcessors() {
        ProcessorChain processorChain = new ProcessorChain("id", null);
        processorChain.execute(mock(MutableExecutionContext.class), ExecutionPhase.REQUEST).test().assertResult();
    }

    @Test
    public void shouldCompleteWithEmptyProcessors() {
        ProcessorChain processorChain = new ProcessorChain("id", List.of());
        processorChain.execute(mock(MutableExecutionContext.class), ExecutionPhase.REQUEST).test().assertResult();
    }

    @Test
    public void shouldCompleteWithCompleteProcessor() {
        when(mockProcessor.execute(any())).thenReturn(Completable.complete());
        ProcessorChain processorChain = new ProcessorChain("id", List.of(mockProcessor));
        processorChain.execute(mock(MutableExecutionContext.class), ExecutionPhase.REQUEST).test().assertResult();
    }

    @Test
    public void shouldCompleteWithCompleteProcessors() {
        when(mockProcessor.execute(any()))
            .thenReturn(Completable.complete())
            .thenReturn(Completable.complete())
            .thenReturn(Completable.complete());
        ProcessorChain processorChain = new ProcessorChain("id", List.of(mockProcessor, mockProcessor, mockProcessor));
        processorChain.execute(mock(MutableExecutionContext.class), ExecutionPhase.REQUEST).test().assertResult();
        verify(mockProcessor, times(3)).execute(any());
    }

    @Test
    public void shouldFailImmediatelyWithFailedProcessor() {
        when(mockProcessor.execute(any()))
            .thenReturn(Completable.complete())
            .thenReturn(Completable.error(new RuntimeException()))
            .thenReturn(Completable.complete());
        ProcessorChain processorChain = new ProcessorChain("id", List.of(mockProcessor, mockProcessor, mockProcessor));
        processorChain.execute(mock(MutableExecutionContext.class), ExecutionPhase.REQUEST).test().assertError(RuntimeException.class);

        verify(mockProcessor, times(2)).execute(any());
    }
}
