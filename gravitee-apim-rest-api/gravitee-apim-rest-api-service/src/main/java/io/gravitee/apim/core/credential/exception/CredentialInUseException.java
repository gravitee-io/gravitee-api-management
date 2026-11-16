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
package io.gravitee.apim.core.credential.exception;

import io.gravitee.apim.core.exception.ConflictDomainException;
import java.util.List;
import java.util.stream.Collectors;

public class CredentialInUseException extends ConflictDomainException {

    private static final int NAMED_CONSUMERS = 10;

    public CredentialInUseException(String credentialId, List<String> consumers) {
        super(message(consumers), credentialId);
    }

    private static String message(List<String> consumers) {
        var named = consumers.stream().limit(NAMED_CONSUMERS).collect(Collectors.joining(", "));
        var more = consumers.size() > NAMED_CONSUMERS ? " and " + (consumers.size() - NAMED_CONSUMERS) + " more" : "";
        return "The credential is still used by " + named + more + ". Remove it from them before you delete it.";
    }
}
