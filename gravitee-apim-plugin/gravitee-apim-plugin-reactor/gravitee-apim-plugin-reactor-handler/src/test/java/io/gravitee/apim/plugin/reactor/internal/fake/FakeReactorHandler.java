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
package io.gravitee.apim.plugin.reactor.internal.fake;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@Getter
public class FakeReactorHandler implements ApiReactor<FakeReactable> {

    @Override
    public List<Acceptor<?>> acceptors() {
        return null;
    }

    @Override
    public FakeReactable api() {
        return null;
    }

    @Override
    public Completable handle(MutableExecutionContext ctx) {
        return null;
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return null;
    }

    @Override
    public ReactorHandler start() throws Exception {
        return null;
    }

    @Override
    public ReactorHandler stop() throws Exception {
        return null;
    }
}
