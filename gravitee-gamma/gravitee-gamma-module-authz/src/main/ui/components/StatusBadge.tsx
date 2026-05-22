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
import { Badge } from '@gravitee/graphene-core';
import type { PolicyStatus } from '../lib/api/authz-api.types';

const MAP: Record<PolicyStatus, { label: string; variant: 'default' | 'outline' | 'destructive' | 'secondary' }> = {
    DRAFT: { label: 'Draft', variant: 'outline' },
    DEPLOYED: { label: 'Deployed', variant: 'default' },
    DISABLED: { label: 'Disabled', variant: 'secondary' },
};

export function StatusBadge({ status }: { status: PolicyStatus }) {
    const { label, variant } = MAP[status];
    return <Badge variant={variant}>{label}</Badge>;
}
