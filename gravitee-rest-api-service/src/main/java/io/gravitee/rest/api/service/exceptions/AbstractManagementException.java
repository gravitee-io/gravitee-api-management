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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractManagementException extends RuntimeException {

    /**
     * Constructor.
     */
    public AbstractManagementException() {
    }

    /**
     * Constructor.
     * @param cause the exception cause
     */
    public AbstractManagementException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     * @param message the exception message
     */
    public AbstractManagementException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * @param message the exception message
     * @param cause the exception cause
     */
    public AbstractManagementException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract int getHttpStatusCode();
}
