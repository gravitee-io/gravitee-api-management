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
import { Badge, Label } from '@gravitee/graphene-core';
import { PlusIcon, XIcon } from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';
import { useCallback, useMemo, useState } from 'react';

import { InfoTooltip } from './InfoTooltip';

export interface ChipsProps {
    label: string;
    hint: ReactNode;
    values: string[];
    placeholder: string;
    disabled?: boolean;
    suggestions?: string[];
    onChange: (next: string[]) => void;
}

export function Chips({ label, hint, values, placeholder, disabled, suggestions, onChange }: ChipsProps) {
    const [draft, setDraft] = useState('');

    const add = (v: string) => {
        const trimmed = v.trim();
        if (!trimmed || values.includes(trimmed)) return;
        onChange([...values, trimmed]);
        setDraft('');
    };

    const remove = useCallback((v: string) => onChange(values.filter(x => x !== v)), [onChange, values]);

    const filteredSuggestions = useMemo(() => (suggestions ?? []).filter(s => !values.includes(s)), [suggestions, values]);

    return (
        <div className="space-y-2">
            <div className="flex items-center gap-1.5">
                <Label className={disabled ? 'text-muted-foreground' : ''}>{label}</Label>
                <InfoTooltip content={hint} />
            </div>

            <div className="flex flex-wrap gap-2 rounded-md border bg-muted/30 p-2 min-h-11">
                {values.map(v => (
                    <Badge key={v} variant="secondary" className="gap-1 font-mono text-xs">
                        {v}
                        {!disabled && (
                            <button type="button" onClick={() => remove(v)} className="hover:text-destructive" aria-label={`Remove ${v}`}>
                                <XIcon className="size-3" />
                            </button>
                        )}
                    </Badge>
                ))}
                <input
                    className="flex-1 min-w-36 bg-transparent outline-none text-sm placeholder:text-muted-foreground disabled:cursor-not-allowed"
                    aria-label={label}
                    placeholder={values.length === 0 ? placeholder : ''}
                    value={draft}
                    disabled={disabled}
                    onChange={e => setDraft(e.target.value)}
                    onKeyDown={e => {
                        if (e.key === 'Enter' || e.key === ',') {
                            e.preventDefault();
                            add(draft);
                        } else if (e.key === 'Backspace' && !draft && values.length) {
                            remove(values[values.length - 1]);
                        }
                    }}
                    onBlur={() => {
                        if (draft.trim()) add(draft);
                    }}
                />
            </div>

            {!disabled && filteredSuggestions.length > 0 && (
                <div className="flex flex-wrap items-center gap-1.5">
                    <span className="text-xs text-muted-foreground">Suggested:</span>
                    {filteredSuggestions.map(s => (
                        <button
                            key={s}
                            type="button"
                            onClick={() => add(s)}
                            className="inline-flex items-center gap-0.5 text-xs text-primary hover:underline"
                        >
                            <PlusIcon className="size-3" />
                            {s}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}
