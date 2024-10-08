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
package io.gravitee.gateway.reactive.reactor.processor.transaction;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.reactivex.rxjava3.core.Completable;

/**
 * A {@link Request} processor used to set the transaction ID of the request.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransactionPreProcessor implements Processor {

    private final String transactionHeader;
    private final String requestHeader;

    TransactionPreProcessor(final String transactionHeader, final String requestHeader) {
        this.transactionHeader = transactionHeader;
        this.requestHeader = requestHeader;
    }

    String transactionHeader() {
        return transactionHeader;
    }

    String requestHeader() {
        return requestHeader;
    }

    @Override
    public String getId() {
        return "pre-processor-transaction";
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            final String requestId = ctx.request().id();
            String transactionId = ctx.request().headers().get(transactionHeader);
            if (transactionId == null) {
                transactionId = requestId;
                ctx.request().headers().set(transactionHeader, transactionId);
            }
            ctx.request().headers().set(requestHeader, requestId);

            ctx.request().transactionId(transactionId);
        });
    }
}
