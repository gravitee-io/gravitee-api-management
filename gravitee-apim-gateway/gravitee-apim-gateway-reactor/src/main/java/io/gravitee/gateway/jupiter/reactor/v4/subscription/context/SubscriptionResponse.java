/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.reactor.v4.subscription.context;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.core.MessageFlow;
import io.gravitee.gateway.jupiter.core.context.AbstractResponse;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.reactivex.rxjava3.core.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionResponse extends AbstractResponse implements MutableResponse {

    public SubscriptionResponse() {
        this.statusCode = HttpStatusCode.OK_200;
        this.headers = HttpHeaders.create();
        this.messageFlow = new MessageFlow();
    }

    @Override
    public Maybe<Buffer> body() {
        // Subscription does not allow buffer body access.
        return Maybe.empty();
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        // Subscription does not allow buffer body access.
        return Single.just(Buffer.buffer());
    }

    @Override
    public void body(Buffer buffer) {
        // Subscription does not allow buffer body access.
    }

    @Override
    public Completable onBody(MaybeTransformer<Buffer, Buffer> onBody) {
        // Subscription does not allow buffer body access.
        return Completable.complete();
    }
}
