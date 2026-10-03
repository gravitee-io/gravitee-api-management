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
package io.gravitee.gateway.services.sync.process.distributed.model;

/**
 * A distributed event could not be written to the distributed sync repository (the repository was unreachable
 * or kept failing after its own retries). Retrying later can succeed, so this is classified as transient.
 */
public class DistributedSyncTransientException extends DistributedSyncException {

    public DistributedSyncTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
