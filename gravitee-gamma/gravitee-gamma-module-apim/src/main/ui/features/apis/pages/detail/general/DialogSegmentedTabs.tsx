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

export interface DialogTabItem<T extends string> {
    id: T;
    label: string;
}

interface DialogSegmentedTabsProps<T extends string> {
    tabs: readonly DialogTabItem<T>[];
    activeId: T;
    onChange: (id: T) => void;
    ariaLabel?: string;
}

/** Equal-width tab strip for compact action dialogs (Export API, etc.). */
export function DialogSegmentedTabs<T extends string>({
    tabs,
    activeId,
    onChange,
    ariaLabel = 'Dialog sections',
}: Readonly<DialogSegmentedTabsProps<T>>) {
    return (
        <div
            className="grid gap-1 border-b pb-0"
            style={{ gridTemplateColumns: `repeat(${tabs.length}, minmax(0, 1fr))` }}
            role="tablist"
            aria-label={ariaLabel}
        >
            {tabs.map(item => (
                <button
                    key={item.id}
                    type="button"
                    role="tab"
                    aria-selected={activeId === item.id}
                    className={cn(
                        'min-w-0 px-2 py-2.5 text-xs sm:text-sm font-medium text-center leading-snug transition-colors border-b-2 -mb-px',
                        activeId === item.id
                            ? 'border-primary text-primary'
                            : 'border-transparent text-muted-foreground hover:text-foreground',
                    )}
                    onClick={() => onChange(item.id)}
                >
                    {item.label}
                </button>
            ))}
        </div>
    );
}
