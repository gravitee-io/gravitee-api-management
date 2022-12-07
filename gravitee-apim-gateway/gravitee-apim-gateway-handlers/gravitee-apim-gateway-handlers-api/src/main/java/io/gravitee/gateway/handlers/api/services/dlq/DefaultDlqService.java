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
package io.gravitee.gateway.handlers.api.services.dlq;

import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.service.dlq.DlqService;
import io.reactivex.rxjava3.core.Flowable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class DefaultDlqService implements DlqService {

    private final EndpointAsyncConnector endpointConnector;

    DefaultDlqService(EndpointAsyncConnector endpointConnector) {
        this.endpointConnector = endpointConnector;
    }

    public Flowable<Message> apply(Flowable<Message> messages) {
        return messages
            .groupBy(Message::error)
            .flatMap(
                grouped -> {
                    if (Boolean.TRUE.equals(grouped.getKey())) {
                        final DlqExecutionContext dlqCtx = new DlqExecutionContext(grouped);
                        return endpointConnector.publish(dlqCtx).andThen(Flowable.defer(dlqCtx.request()::messages));
                    } else {
                        return grouped;
                    }
                }
            );
    }
}
