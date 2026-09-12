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
import { apimFetchJsonOrg } from '../../../shared/api/apimClient';

/**
 * Subset of GET /organizations/{orgId}/console (ConsoleConfigEntity) this module reads. Module-apim
 * and module-platform are separate Module Federation remotes with no shared settings channel, so this
 * declares its own fields rather than importing the platform module's `ConsoleSettings`.
 */
export interface OrgConsoleSettings {
    federation?: {
        enabled?: boolean;
    };
}

export async function fetchOrgConsoleSettings(signal?: AbortSignal): Promise<OrgConsoleSettings> {
    return apimFetchJsonOrg<OrgConsoleSettings>('/console', { signal });
}
