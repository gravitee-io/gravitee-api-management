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
package io.gravitee.apim.core.subscription_form.exception;

import io.gravitee.apim.core.exception.ConflictDomainException;

/**
 * Thrown when deleting the environment default subscription form: another form must be promoted
 * first so every API keeps a form to fall back on.
 *
 * @author Gravitee.io Team
 */
public class SubscriptionFormIsDefaultException extends ConflictDomainException {

    public SubscriptionFormIsDefaultException(String subscriptionFormId) {
        super("The default subscription form cannot be deleted. Set another form as default first.", subscriptionFormId);
    }
}
