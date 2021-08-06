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
package io.gravitee.rest.api.service.exceptions;

import static java.util.Collections.singletonMap;

import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperation;
import java.util.Map;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BridgeOperationException extends AbstractManagementException {

    BridgeOperation operation;

    public BridgeOperationException(BridgeOperation operation) {
        this.operation = operation;
    }

    @Override
    public String getMessage() {
        return "Problem while executing cockpit operation " + operation.name();
    }

    @Override
    public int getHttpStatusCode() {
        return 500;
    }

    @Override
    public String getTechnicalCode() {
        return "cockpit.operation";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("operation", operation.name());
    }
}
