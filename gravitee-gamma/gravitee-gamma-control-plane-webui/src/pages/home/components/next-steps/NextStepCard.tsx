/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { cn } from '@gravitee/graphene-core';

import type { NextStep } from './next-steps';
import { ACCENT_CLASSES } from '../accents';

/** Renders one tile inside the "Suggested next steps" banner. Informational, not navigable. */
export function NextStepCard({ step }: { readonly step: NextStep }) {
    const { Icon, title, description, accent } = step;
    const accentClasses = ACCENT_CLASSES[accent];
    return (
        <div className="flex items-center gap-3 rounded-lg border bg-card p-3">
            <div className={cn('rounded-md p-2 shrink-0', accentClasses.bg)}>
                <Icon className={cn('size-4', accentClasses.fg)} aria-hidden />
            </div>
            <div className="flex-1 min-w-0">
                <h3 className="text-sm font-medium leading-tight">{title}</h3>
                <p className="text-xs text-muted-foreground mt-0.5">{description}</p>
            </div>
        </div>
    );
}
