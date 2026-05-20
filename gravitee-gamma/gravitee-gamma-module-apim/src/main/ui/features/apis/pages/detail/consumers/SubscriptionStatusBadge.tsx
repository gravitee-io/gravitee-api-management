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

import type { SubscriptionStatus } from '../../../types/subscription';

type BadgeVariant = 'default' | 'secondary' | 'destructive' | 'outline';

interface StatusConfig {
    label: string;
    variant?: BadgeVariant;
    className?: string;
}

const STATUS_CONFIG: Record<SubscriptionStatus, StatusConfig> = {
    ACCEPTED: { label: 'Accepted', className: 'bg-success/10 text-success border-transparent' },
    RESUMED: { label: 'Resumed', className: 'bg-success/10 text-success border-transparent' },
    PENDING: { label: 'Pending', variant: 'outline', className: 'border-warning/30 text-warning' },
    PAUSED: { label: 'Paused', variant: 'outline', className: 'border-warning/30 text-warning' },
    REJECTED: { label: 'Rejected', variant: 'destructive' },
    CLOSED: { label: 'Closed', variant: 'secondary' },
};

export function SubscriptionStatusBadge({ status }: { status: SubscriptionStatus }) {
    const config = STATUS_CONFIG[status] ?? { label: status, variant: 'outline' as BadgeVariant };
    return (
        <Badge variant={config.variant} className={config.className}>
            {config.label}
        </Badge>
    );
}
