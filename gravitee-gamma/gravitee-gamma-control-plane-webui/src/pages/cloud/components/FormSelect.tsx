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
import { ChevronDownIcon } from '@gravitee/graphene-core/icons';
import type { SelectHTMLAttributes } from 'react';

/** Matches Graphene `Input` field height and spacing for aligned form controls. */
const FORM_SELECT_CLASS =
    'h-9 w-full min-w-0 appearance-none rounded-md border border-input bg-background py-0 pl-3 pr-9 text-sm leading-9 text-foreground shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50';

export function FormSelect({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
    return (
        <div className="relative w-full">
            <select className={className ? `${FORM_SELECT_CLASS} ${className}` : FORM_SELECT_CLASS} {...props}>
                {children}
            </select>
            <ChevronDownIcon
                className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-muted-foreground"
                aria-hidden
            />
        </div>
    );
}
