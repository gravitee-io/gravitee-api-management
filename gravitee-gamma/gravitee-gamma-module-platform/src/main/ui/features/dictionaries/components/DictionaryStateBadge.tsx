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

import type { DictionaryLifecycleState } from '../types/dictionary';

const STATE_CONFIG: Record<DictionaryLifecycleState, { label: string; className: string }> = {
    STARTED: { label: 'Started', className: 'bg-green-100 text-green-700 border-transparent' },
    STOPPED: { label: 'Stopped', className: 'bg-muted text-muted-foreground border-transparent' },
};

export function DictionaryStateBadge({ state }: Readonly<{ state: DictionaryLifecycleState | undefined }>) {
    if (!state) {
        return <span className="text-sm text-muted-foreground">—</span>;
    }
    const config = STATE_CONFIG[state] ?? { label: state, className: '' };
    return <Badge className={cn('font-normal text-xs', config.className)}>{config.label}</Badge>;
}
