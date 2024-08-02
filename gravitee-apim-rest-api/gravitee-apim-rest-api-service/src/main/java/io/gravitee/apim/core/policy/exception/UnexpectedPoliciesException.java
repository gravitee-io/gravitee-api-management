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
package io.gravitee.apim.core.policy.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.List;

public class UnexpectedPoliciesException extends ValidationDomainException {

    public UnexpectedPoliciesException(List<String> policyNames, String apiType, String phase) {
        super(String.format("Unexpected policies [%s] for API type %s and phase %s", String.join(", ", policyNames), apiType, phase));
    }
}
