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

import type { PlanStatus } from '../../features/apis/types/plan';

const STATUS_CONFIG: Record<PlanStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }> = {
    STAGING: { label: 'Staging', variant: 'outline' },
    PUBLISHED: { label: 'Published', variant: 'default' },
    DEPRECATED: { label: 'Deprecated', variant: 'secondary' },
    CLOSED: { label: 'Closed', variant: 'destructive' },
};

interface PlanStatusBadgeProps {
    status: PlanStatus;
}

export function PlanStatusBadge({ status }: Readonly<PlanStatusBadgeProps>) {
    const { label, variant } = STATUS_CONFIG[status];
    return <Badge variant={variant}>{label}</Badge>;
}
