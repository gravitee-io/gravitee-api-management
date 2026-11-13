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

export function CredentialStepMarker({
    index,
    label,
    current,
    done,
}: Readonly<{ index: number; label: string; current: boolean; done: boolean }>) {
    return (
        <li className="flex items-center gap-2" aria-current={current ? 'step' : undefined}>
            <span
                className={cn(
                    'flex size-6 items-center justify-center rounded-full text-xs font-medium',
                    current || done ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground',
                )}
            >
                {done ? '✓' : index}
            </span>
            <span className={cn(!current && 'text-muted-foreground', current && 'font-medium')}>{label}</span>
        </li>
    );
}
