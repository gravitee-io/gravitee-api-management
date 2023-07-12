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
package io.gravitee.gateway.jupiter.handlers.api.processor.transaction;

/**
 * The different modes available in the Gateway when it deals with a backend providing a value for the Transaction Id and Request Id headers
 */
public enum TransactionHeaderOverrideMode {
    /**
     * OVERRIDE: The header set by the APIM Gateway will override the one provided by the backend
     */
    OVERRIDE,
    /**
     * MERGE: Both headers set by the APIM Gateway and the backend will be kept (as headers can be multivalued)
     */
    MERGE,
    /**
     * KEEP: The header set by the backend will be kept and the one provided by the APIM Gateway discarded
     */
    KEEP,
}
