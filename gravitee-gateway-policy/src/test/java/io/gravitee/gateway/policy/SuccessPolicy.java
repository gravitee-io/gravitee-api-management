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
package io.gravitee.gateway.policy;

import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.policy.api.PolicyChain;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class SuccessPolicy implements Policy {

    @Override
    public void onRequest(Object... args) throws Exception {
        ((PolicyChain)args[2]).doNext(null, null);
    }

    @Override
    public void onResponse(Object... args) throws Exception {
        ((PolicyChain)args[2]).doNext(null, null);
    }

    @Override
    public ReadWriteStream<?> onRequestContent(Object... args) throws Exception {
        return null;
    }

    @Override
    public ReadWriteStream<?> onResponseContent(Object... args) throws Exception {
        return null;
    }

    @Override
    public boolean isStreamable() {
        return false;
    }

    @Override
    public boolean isRunnable() {
        return true;
    }
}
