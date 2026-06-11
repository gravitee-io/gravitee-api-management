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
import { Input } from '@gravitee/graphene-core';
import { useRef, useState } from 'react';

import { PluginIcon } from '../../../../../components/PluginIcon';

export interface ApiResourceOption {
    name: string;
    type: string;
    icon?: string;
}

interface ResourceSelectInputProps {
    id: string;
    value: string;
    onChange: (value: string) => void;
    /** Eligible resources already filtered for this field (e.g. oauth2 or cache). */
    options: readonly ApiResourceOption[];
    placeholder?: string;
    disabled?: boolean;
    'aria-describedby'?: string;
}

/**
 * Free-text input with an autocomplete of the API's configured resources. Free text is preserved
 * (the field supports EL), while matching resources are suggested with their plugin icon — mirroring
 * the classic console's resource picker.
 */
export function ResourceSelectInput({
    id,
    value,
    onChange,
    options,
    placeholder,
    disabled,
    'aria-describedby': ariaDescribedBy,
}: Readonly<ResourceSelectInputProps>) {
    const [open, setOpen] = useState(false);
    const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

    const query = value.trim().toLowerCase();
    const suggestions = query ? options.filter(o => o.name.toLowerCase().includes(query)) : options;
    const listId = `${id}-listbox`;

    const cancelClose = () => {
        if (closeTimer.current) clearTimeout(closeTimer.current);
    };

    const select = (name: string) => {
        cancelClose();
        onChange(name);
        setOpen(false);
    };

    return (
        <div className="relative">
            <Input
                id={id}
                role="combobox"
                aria-expanded={open}
                aria-controls={listId}
                aria-autocomplete="list"
                aria-describedby={ariaDescribedBy}
                autoComplete="off"
                value={value}
                disabled={disabled}
                placeholder={placeholder}
                onChange={e => {
                    onChange(e.target.value);
                    setOpen(true);
                }}
                onFocus={() => setOpen(true)}
                onBlur={() => {
                    closeTimer.current = setTimeout(() => setOpen(false), 120);
                }}
            />
            {open && options.length > 0 && (
                <ul
                    id={listId}
                    role="listbox"
                    className="absolute z-50 mt-1 max-h-56 w-full overflow-auto rounded-md border bg-popover p-1 shadow-md"
                    onMouseDown={cancelClose}
                >
                    {suggestions.length === 0 ? (
                        <li className="px-2 py-2 text-xs text-muted-foreground">No matching resource</li>
                    ) : (
                        suggestions.map(o => (
                            <li key={o.name}>
                                <button
                                    type="button"
                                    role="option"
                                    aria-selected={o.name === value}
                                    className="flex w-full items-center gap-2 rounded-sm px-2 py-1.5 text-left text-sm hover:bg-accent"
                                    onClick={() => select(o.name)}
                                >
                                    <PluginIcon icon={o.icon} className="size-5" />
                                    <span className="truncate">{o.name}</span>
                                    <span className="ml-auto shrink-0 text-xs text-muted-foreground">{o.type}</span>
                                </button>
                            </li>
                        ))
                    )}
                </ul>
            )}
        </div>
    );
}
