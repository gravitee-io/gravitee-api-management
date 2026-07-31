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

export interface SelectionCardItem<T extends string> {
    id: T;
    label: string;
    description?: string;
}

interface SelectionCardsProps<T extends string> {
    options: readonly SelectionCardItem<T>[];
    activeId: T;
    onChange: (id: T) => void;
    ariaLabel?: string;
}

/** Boxed radio-card picker for nested choices, avoiding stacking tabs inside tabs. */
export function SelectionCards<T extends string>({ options, activeId, onChange, ariaLabel = 'Options' }: Readonly<SelectionCardsProps<T>>) {
    return (
        <div
            className="grid gap-2"
            style={{ gridTemplateColumns: `repeat(${options.length}, minmax(0, 1fr))` }}
            role="radiogroup"
            aria-label={ariaLabel}
        >
            {options.map(option => {
                const selected = activeId === option.id;
                return (
                    <button
                        key={option.id}
                        type="button"
                        role="radio"
                        aria-checked={selected}
                        aria-label={option.label}
                        onClick={() => onChange(option.id)}
                        className={cn(
                            'flex items-start gap-2.5 rounded-lg border p-3 text-left transition-colors',
                            selected ? 'border-primary bg-primary/5' : 'hover:bg-muted/50',
                        )}
                    >
                        <span
                            className={cn(
                                'mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-full border-2',
                                selected ? 'border-primary' : 'border-muted-foreground/40',
                            )}
                        >
                            {selected && <span className="size-2 rounded-full bg-primary" />}
                        </span>
                        <div className="min-w-0">
                            <p className={cn('text-sm font-medium', selected && 'text-primary')}>{option.label}</p>
                            {option.description && <p className="text-xs text-muted-foreground">{option.description}</p>}
                        </div>
                    </button>
                );
            })}
        </div>
    );
}
