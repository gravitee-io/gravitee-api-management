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
package io.gravitee.apim.core.basic_auth.model;

/**
 * Thread-local holder for plain-text Basic Auth credentials generated during subscription acceptance.
 * The plain password is only available once (during the same request that creates the subscription)
 * and must be cleared after use.
 */
public final class BasicAuthPlainCredentialsHolder {

    private static final ThreadLocal<BasicAuthCredentialsEntity> HOLDER = new ThreadLocal<>();

    private BasicAuthPlainCredentialsHolder() {}

    public static void set(BasicAuthCredentialsEntity credentials) {
        HOLDER.set(credentials);
    }

    public static BasicAuthCredentialsEntity getAndClear() {
        var creds = HOLDER.get();
        HOLDER.remove();
        return creds;
    }
}
