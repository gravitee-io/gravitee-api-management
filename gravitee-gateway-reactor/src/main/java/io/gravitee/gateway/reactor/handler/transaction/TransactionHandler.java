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
package io.gravitee.gateway.reactor.handler.transaction;

import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;

/**
 * A {@link Request} handler used to set the transaction ID of the request.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransactionHandler implements Handler<Request> {

    final static String DEFAULT_TRANSACTIONAL_ID_HEADER = "X-Gravitee-Transaction-Id";

    private final Handler<Request> next;
    private final Response response;

    private String transactionHeader = DEFAULT_TRANSACTIONAL_ID_HEADER;

    TransactionHandler(final Handler<Request> next, final Response response) {
        this.next = next;
        this.response = response;
    }

    TransactionHandler(String transactionHeader, final Handler<Request> next, final Response response) {
        this(next,response);
        this.transactionHeader = transactionHeader;
    }

    @Override
    public void handle(Request request) {
        String transactionId = request.headers().getFirst(transactionHeader);
        if (transactionId == null) {
            transactionId = UUID.toString(UUID.random());
            request.headers().set(transactionHeader, transactionId);
        }
        response.headers().set(transactionHeader,transactionId);

        request.metrics().setTransactionId(transactionId);
        next.handle(new TransactionRequest(transactionId, request));
    }
}
