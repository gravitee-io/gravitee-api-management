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

import { Badge, cn } from '@gravitee/graphene-core';

import type { DictionaryType } from '../types/dictionary';

type BadgeVariant = 'default' | 'secondary' | 'destructive' | 'outline';

const TYPE_CONFIG: Record<DictionaryType, { label: string; variant: BadgeVariant; className?: string }> = {
    MANUAL: { label: 'Manual', variant: 'outline', className: 'text-muted-foreground' },
    DYNAMIC: { label: 'Dynamic', variant: 'outline', className: 'border-primary/30 text-primary' },
};

export function DictionaryTypeBadge({ type }: Readonly<{ type: DictionaryType }>) {
    const config = TYPE_CONFIG[type] ?? { label: type, variant: 'outline' as BadgeVariant };
    return (
        <Badge variant={config.variant} className={cn('font-normal text-xs', config.className)}>
            {config.label}
        </Badge>
    );
}
