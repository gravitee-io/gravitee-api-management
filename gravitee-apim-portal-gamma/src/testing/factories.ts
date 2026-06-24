/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import type { BootstrapConfig } from '../shared/config/bootstrap.store';

export const TEST_CONFIG = {
    baseURL: 'http://api.test/portal',
    organizationId: 'test-org',
    environmentId: 'DEFAULT',
} as const;

export function buildBootstrapConfig(overrides: Partial<BootstrapConfig> = {}): BootstrapConfig {
    return { ...TEST_CONFIG, ...overrides };
}
