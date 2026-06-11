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
import { cn } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

type ReviewRowProps =
    | { label: string; value: string; mono?: boolean; children?: never }
    | { label: string; children: ReactNode; value?: never; mono?: never };

/** Label/value row for wizard review steps. Rows stack inside a bordered container. */
export function ReviewRow({ label, value, mono, children }: ReviewRowProps) {
    return (
        <div className="flex items-start justify-between gap-4 px-4 py-2.5 border-b last:border-b-0">
            <span className="text-xs text-muted-foreground shrink-0">{label}</span>
            {children ?? (
                <span className={cn('max-w-[60%] break-words text-right text-xs font-medium', mono && 'font-mono')}>
                    {value || <span className="text-muted-foreground italic font-sans">Not set</span>}
                </span>
            )}
        </div>
    );
}
