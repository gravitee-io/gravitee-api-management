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

export function ChipInput({
    id,
    values,
    onChange,
    placeholder,
}: Readonly<{ id?: string; values: string[]; onChange: (next: string[]) => void; placeholder: string }>) {
    const [draft, setDraft] = useState('');

    const add = (v: string) => {
        const trimmed = v.trim();
        if (!trimmed || values.includes(trimmed)) return;
        onChange([...values, trimmed]);
        setDraft('');
    };

    const remove = (v: string) => onChange(values.filter(x => x !== v));

    return (
        <div className="flex flex-wrap gap-1.5 rounded-md border bg-muted/30 p-2" style={{ minHeight: '38px' }}>
            {values.map(v => (
                <Badge key={v} variant="secondary" style={{ fontSize: '11px', gap: '2px' }}>
                    {v}
                    <button type="button" onClick={() => remove(v)} className="hover:text-destructive ml-0.5" aria-label={`Remove ${v}`}>
                        <XIcon className="size-3" />
                    </button>
                </Badge>
            ))}
            <input
                id={id}
                className="flex-1 bg-transparent outline-none text-sm"
                style={{ minWidth: '100px' }}
                placeholder={placeholder}
                value={draft}
                onChange={e => setDraft(e.target.value)}
                onKeyDown={e => {
                    if (e.key === 'Enter' || e.key === ',') {
                        e.preventDefault();
                        add(draft);
                    } else if (e.key === 'Backspace' && !draft && values.length) {
                        remove(values[values.length - 1]);
                    }
                }}
            />
        </div>
    );
}
