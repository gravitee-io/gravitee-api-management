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
import { Badge } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

export interface ChipInputProps {
    readonly id?: string;
    readonly values: string[];
    readonly onChange: (next: string[]) => void;
    readonly placeholder: string;
    /** When true, pressing comma also commits the current draft value (off by default for URI-like values). */
    readonly addOnComma?: boolean;
}

export function ChipInput({ id, values, onChange, placeholder, addOnComma = false }: ChipInputProps) {
    const [draft, setDraft] = useState('');

    const add = (value: string) => {
        const trimmed = value.trim();
        if (!trimmed || values.includes(trimmed)) {
            return;
        }
        onChange([...values, trimmed]);
        setDraft('');
    };

    const remove = (value: string) => onChange(values.filter(item => item !== value));

    return (
        <div className="flex min-h-9 flex-wrap gap-1.5 rounded-md border bg-muted/30 p-2">
            {values.map(value => (
                <Badge key={value} variant="secondary" className="gap-0.5 pr-1 text-[11px] font-normal">
                    {value}
                    <button
                        type="button"
                        onClick={() => remove(value)}
                        className="ml-0.5 rounded-sm p-0.5 hover:text-destructive"
                        aria-label={`Remove ${value}`}
                    >
                        <XIcon className="size-3" aria-hidden />
                    </button>
                </Badge>
            ))}
            <input
                id={id}
                className="min-w-[100px] flex-1 bg-transparent text-sm outline-none"
                placeholder={placeholder}
                value={draft}
                onChange={event => setDraft(event.target.value)}
                onKeyDown={event => {
                    if (event.key === 'Enter' || (addOnComma && event.key === ',')) {
                        event.preventDefault();
                        add(draft);
                    } else if (event.key === 'Backspace' && !draft && values.length > 0) {
                        remove(values[values.length - 1]!);
                    }
                }}
                onBlur={() => add(draft)}
            />
        </div>
    );
}
