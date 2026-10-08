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
import type { SharedConfigFormState } from '../pages/detail/endpoints/types';
import type { EndpointGroupSharedConfiguration } from '../types';

/**
 * Shared configuration is schema-driven: form values are already shaped by the
 * endpoint plugin's shared-configuration JSON Schema. Persist as-is.
 */
export function serializeSharedConfiguration(config: SharedConfigFormState): EndpointGroupSharedConfiguration {
    return { ...config } as EndpointGroupSharedConfiguration;
}

export function serializeSharedConfigurationOverride(config: SharedConfigFormState | undefined): Record<string, unknown> {
    if (!config) return {};
    return serializeSharedConfiguration(config) as Record<string, unknown>;
}
