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
package io.gravitee.apim.integration.tests.fake;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.context.MessageExecutionContext;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Policy that replaces the content of one subscribed message with bytes that are not valid UTF-8.
 */
public class InvalidUtf8MessagePolicy implements Policy {

    public static final String ID = "invalid-utf8-message";
    public static final String CORRUPTED_MESSAGE_ID = "2";

    private static final byte[] INVALID_UTF8 = { (byte) 0xC3, (byte) 0x28 };

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Completable onMessageResponse(MessageExecutionContext ctx) {
        return ctx.response().onMessage(message -> Maybe.just(corruptIfTargeted(message)));
    }

    private static Message corruptIfTargeted(Message message) {
        if (CORRUPTED_MESSAGE_ID.equals(message.id())) {
            return message.content(Buffer.buffer(INVALID_UTF8));
        }
        return message;
    }
}
