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
import type { ApiProtocolType, Policy, PolicyDocumentation } from '@gravitee/graphene-policy-studio';

import { apimFetchJsonV2Org } from '../api/apimClient';

/**
 * The policy plugin catalog, shared by every Policy Studio host in this module. The endpoint is
 * organization-scoped and returns the whole catalog: it is the caller that narrows it down, either by
 * passing its own protocol below or by letting the studio filter the palette.
 */
export function listPolicies(): Promise<Policy[]> {
    return apimFetchJsonV2Org<Policy[]>('/plugins/policies');
}

/** Schemas are protocol-specific: the same policy exposes different fields on a proxy and on a message API. */
export function getPolicySchema(policyId: string, protocolType: ApiProtocolType): Promise<unknown> {
    return apimFetchJsonV2Org<unknown>(
        `/plugins/policies/${encodeURIComponent(policyId)}/schema?apiProtocolType=${encodeURIComponent(protocolType)}`,
    );
}

export function getPolicyDocumentation(policyId: string, protocolType: ApiProtocolType): Promise<PolicyDocumentation> {
    return apimFetchJsonV2Org<PolicyDocumentation>(
        `/plugins/policies/${encodeURIComponent(policyId)}/documentation-ext?apiProtocolType=${encodeURIComponent(protocolType)}`,
    );
}
