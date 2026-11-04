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
package io.gravitee.apim.core.async_job.model;

import jakarta.annotation.Nullable;

/**
 * A failure on one item processed by a job.
 *
 * <p>The message is cut to {@link #MAX_MESSAGE_LENGTH} characters, the size the job-level error message is stored with.
 *
 * @param itemId the id of the failed item, when the failure is tied to one
 */
public record AsyncJobError(String code, String message, @Nullable String itemId) {
    public static final int MAX_MESSAGE_LENGTH = 256;

    public AsyncJobError {
        if (message != null && message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH);
        }
    }
}
