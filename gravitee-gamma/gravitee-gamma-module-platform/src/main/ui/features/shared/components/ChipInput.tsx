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
import { Badge, Button } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

export interface ChipInputProps {
    readonly id?: string;
    readonly values: string[];
    readonly onChange: (next: string[]) => void;
    readonly placeholder: string;
    readonly disabled?: boolean;
    /** When true, pressing comma also commits the current draft value (off by default for URI-like values). */
    readonly addOnComma?: boolean;
}

export function ChipInput({ id, values, onChange, placeholder, disabled = false, addOnComma = false }: ChipInputProps) {
    const [draft, setDraft] = useState('');

    const add = (value: string) => {
        if (disabled) {
            return;
        }
        const trimmed = value.trim();
        if (!trimmed || values.includes(trimmed)) {
            return;
        }
        onChange([...values, trimmed]);
        setDraft('');
    };

    const removeAt = (index: number) => {
        if (disabled) {
            return;
        }
        onChange(values.filter((_, itemIndex) => itemIndex !== index));
    };

    return (
        <div className={`flex min-h-9 flex-wrap gap-1.5 rounded-md border bg-muted/30 p-2 ${disabled ? 'opacity-50' : ''}`}>
            {values.map((value, index) => (
                <Badge key={`${value}-${index}`} variant="secondary" className="gap-0.5 pr-1 text-[11px] font-normal">
                    {value}
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        className="ml-0.5 shrink-0 hover:text-destructive"
                        onClick={() => removeAt(index)}
                        aria-label={`Remove ${value}`}
                        disabled={disabled}
                    >
                        <XIcon className="size-3" aria-hidden />
                    </Button>
                </Badge>
            ))}
            <input
                id={id}
                className="min-w-[100px] flex-1 bg-transparent text-sm outline-none"
                placeholder={placeholder}
                value={draft}
                disabled={disabled}
                onChange={event => setDraft(event.target.value)}
                onKeyDown={event => {
                    if (event.key === 'Enter' || (addOnComma && event.key === ',')) {
                        event.preventDefault();
                        add(draft);
                    } else if (event.key === 'Backspace' && !draft && values.length > 0) {
                        removeAt(values.length - 1);
                    }
                }}
                onBlur={() => add(draft)}
            />
        </div>
    );
}
