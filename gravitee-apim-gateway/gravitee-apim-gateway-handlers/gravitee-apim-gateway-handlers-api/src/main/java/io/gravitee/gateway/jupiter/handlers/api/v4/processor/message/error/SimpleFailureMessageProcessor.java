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
package io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SimpleFailureMessageProcessor extends AbstractFailureMessageProcessor {

    public static final String ID = "message-processor-simple-failure";

    private SimpleFailureMessageProcessor() {}

    public static SimpleFailureMessageProcessor instance() {
        return Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    private static class Holder {

        private static final SimpleFailureMessageProcessor INSTANCE = new SimpleFailureMessageProcessor();
    }
}
