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
package io.gravitee.management.service.exceptions;

import io.gravitee.common.http.HttpStatusCode;

import static java.lang.String.format;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LifecycleStateChangeNotAllowedException extends AbstractManagementException {

    private final String lifecycleState;

    public LifecycleStateChangeNotAllowedException(final String lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return format("The API lifecycle state cannot be changed to %s.", lifecycleState);
    }
}
