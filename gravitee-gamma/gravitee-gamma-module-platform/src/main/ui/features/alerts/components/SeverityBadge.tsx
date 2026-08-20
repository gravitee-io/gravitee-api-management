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

import type { AlertSeverity } from '../types/alert';

type BadgeVariant = 'default' | 'secondary' | 'destructive' | 'outline';

const SEVERITY_CONFIG: Record<AlertSeverity, { label: string; variant: BadgeVariant }> = {
    CRITICAL: { label: 'critical', variant: 'destructive' },
    WARNING: { label: 'warning', variant: 'secondary' },
    INFO: { label: 'info', variant: 'outline' },
};

/** Colored severity pill for the env alerts list (and later create/edit). */
export function SeverityBadge({ severity }: { severity: AlertSeverity }) {
    const config = SEVERITY_CONFIG[severity] ?? { label: severity.toLowerCase(), variant: 'outline' as BadgeVariant };
    return (
        <Badge variant={config.variant} className="text-xs font-normal capitalize">
            {config.label}
        </Badge>
    );
}
