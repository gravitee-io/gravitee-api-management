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
package io.gravitee.gateway.reactive.handlers.api.el;

import io.gravitee.el.spel.context.DeferredFunctionHolder;
import io.gravitee.gateway.handlers.api.manager.CredentialResolver;
import io.gravitee.secrets.api.el.SecretFieldAccessControl;
import io.reactivex.rxjava3.core.Single;

/**
 * The {@code #credentials} EL variable, bound to the environment of one API:
 * {@code {#credentials.get('<credential id>', '<field>', #secret_field_access_control_var)}}.
 *
 * <p>The third argument is the marker a plugin sets while it evaluates a secret field; the resolution is refused
 * without it. EL only calls allow-listed methods, so {@link #get} must be listed in the expression language
 * whitelist for this expression to evaluate.
 */
public final class EvaluatedCredentialsMethods implements DeferredFunctionHolder {

    private final String environmentId;
    private final CredentialResolver credentialResolver;

    public EvaluatedCredentialsMethods(String environmentId, CredentialResolver credentialResolver) {
        this.environmentId = environmentId;
        this.credentialResolver = credentialResolver;
    }

    public Single<String> get(String credentialId, String field, SecretFieldAccessControl accessControl) {
        return Single.fromCallable(() -> credentialResolver.resolve(environmentId, credentialId, field, accessControl));
    }
}
