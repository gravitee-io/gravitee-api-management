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
import type { ApimRuntimeConfig } from '../../core/context/apimRuntimeContext';
import { apimFetchJson } from '../../core/http/apimFetch';
import type { OrgTag } from '../../features/apis/types/api.types';

export async function getOrgTags(runtime: ApimRuntimeConfig): Promise<OrgTag[]> {
    const url = `${runtime.managementBaseURL}/organizations/${runtime.organizationId}/configuration/tags`;
    return apimFetchJson<OrgTag[]>(url);
}
