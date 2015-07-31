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
package io.gravitee.gateway.core.policy.impl;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.policy.PolicyChain;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractPolicyChain implements PolicyChain {

    private boolean isFailure = false;
    private int statusCode = -1;

    @Override
    public void sendError(int statusCode) {
        setFailure();
        this.statusCode = statusCode;
    }

    @Override
    public void sendError(int statusCode, Throwable throwable) {
        sendError(statusCode);
    }

    @Override
    public void sendError(Throwable throwable) {
        sendError(HttpStatusCode.INTERNAL_SERVER_ERROR_500, throwable);
    }

    private void setFailure() {
        isFailure = true;
    }

    public boolean isFailure() {
        return isFailure;
    }

    public int statusCode() {
        return statusCode;
    }
}
