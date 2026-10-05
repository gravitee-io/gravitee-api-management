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
import { useEffect, useRef, useState, type KeyboardEvent } from 'react';

import { integrationProviderLabel, SELECTABLE_PROVIDER_TOKENS } from '../utils/providerLabels';

function nextProviderIndex(currentIndex: number, key: string, providerCount: number): number {
    if (key === 'Home') return 0;
    if (key === 'End') return providerCount - 1;
    if (key === 'ArrowRight' || key === 'ArrowDown') return (currentIndex + 1) % providerCount;
    if (key === 'ArrowLeft' || key === 'ArrowUp') return (currentIndex - 1 + providerCount) % providerCount;
    return currentIndex;
}

export function IntegrationProviderSelector({
    value,
    onChange,
    disabled = false,
}: Readonly<{
    value: string | undefined;
    onChange: (provider: string) => void;
    disabled?: boolean;
}>) {
    const [focusedProvider, setFocusedProvider] = useState<string>(value ?? SELECTABLE_PROVIDER_TOKENS[0]);
    const buttonRefs = useRef<Partial<Record<string, HTMLButtonElement | null>>>({});

    useEffect(() => {
        if (value !== undefined) {
            setFocusedProvider(value);
        }
    }, [value]);

    function handleProviderKeyDown(event: KeyboardEvent<HTMLButtonElement>, provider: string) {
        if (!['ArrowRight', 'ArrowDown', 'ArrowLeft', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
            return;
        }
        event.preventDefault();
        const currentIndex = SELECTABLE_PROVIDER_TOKENS.indexOf(provider);
        const next = SELECTABLE_PROVIDER_TOKENS[nextProviderIndex(currentIndex, event.key, SELECTABLE_PROVIDER_TOKENS.length)];
        setFocusedProvider(next);
        onChange(next);
        buttonRefs.current[next]?.focus();
    }

    return (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2" role="radiogroup" aria-label="Provider">
            {SELECTABLE_PROVIDER_TOKENS.map(provider => {
                const isSelected = value === provider;

                return (
                    <button
                        key={provider}
                        ref={element => {
                            buttonRefs.current[provider] = element;
                        }}
                        type="button"
                        role="radio"
                        tabIndex={focusedProvider === provider ? 0 : -1}
                        aria-checked={isSelected}
                        disabled={disabled}
                        className={cn(
                            'relative flex items-center gap-3 rounded-xl border p-4 text-left outline-none transition-all focus-visible:ring-2 focus-visible:ring-primary/40',
                            isSelected ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/40',
                        )}
                        onKeyDown={event => handleProviderKeyDown(event, provider)}
                        onClick={() => {
                            setFocusedProvider(provider);
                            onChange(provider);
                        }}
                    >
                        <span
                            className={cn(
                                'flex size-4 shrink-0 items-center justify-center rounded-full border',
                                isSelected ? 'border-primary' : 'border-muted-foreground',
                            )}
                            aria-hidden
                        >
                            {isSelected ? <span className="size-2 rounded-full bg-primary" /> : null}
                        </span>
                        <span className="text-sm font-medium">{integrationProviderLabel(provider)}</span>
                    </button>
                );
            })}
        </div>
    );
}
