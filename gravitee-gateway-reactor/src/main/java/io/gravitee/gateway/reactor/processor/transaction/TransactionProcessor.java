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
package io.gravitee.gateway.reactor.processor.transaction;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.core.processor.AbstractProcessor;

/**
 * A {@link Request} processor used to set the transaction ID of the request.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransactionProcessor extends AbstractProcessor<ExecutionContext> {

    final static String DEFAULT_TRANSACTIONAL_ID_HEADER = "X-Gravitee-Transaction-Id";

    private String transactionHeader = DEFAULT_TRANSACTIONAL_ID_HEADER;

    TransactionProcessor() {
    }

    TransactionProcessor(String transactionHeader) {
        this.transactionHeader = transactionHeader;
    }

    @Override
    public void handle(ExecutionContext context) {
        String transactionId = context.request().headers().getFirst(transactionHeader);
        if (transactionId == null) {
            transactionId = context.request().id();
            context.request().headers().set(transactionHeader, transactionId);
        }
        context.response().headers().set(transactionHeader,transactionId);

        context.request().metrics().setTransactionId(transactionId);

        ((MutableExecutionContext)context).request(new TransactionRequest(transactionId, context.request()));

        next.handle(context);
    }
}
