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
import type { ComponentProps } from 'react';
import type { PolicyStatus } from '../shared/api/authz-api.types';

type BadgeVariant = NonNullable<ComponentProps<typeof Badge>['variant']>;

const STATUS_PRESENTATION: Record<PolicyStatus, { readonly label: string; readonly variant: BadgeVariant }> = {
    DRAFT: { label: 'Draft', variant: 'outline' },
    DEPLOYED: { label: 'Deployed', variant: 'success' },
    DISABLED: { label: 'Disabled', variant: 'warning' },
};

export function StatusBadge({ status }: { readonly status: PolicyStatus }) {
    const { label, variant } = STATUS_PRESENTATION[status];
    return <Badge variant={variant}>{label}</Badge>;
}
