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
import { SearchIcon } from '@gravitee/graphene-core/icons';

interface ClientSideTableSearchFieldProps {
    readonly id: string;
    readonly label: string;
    readonly value: string;
    readonly onChange: (value: string) => void;
    readonly placeholder?: string;
}

export function ClientSideTableSearchField({ id, label, value, onChange, placeholder = 'Search' }: ClientSideTableSearchFieldProps) {
    return (
        <div className="relative w-64">
            <SearchIcon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden />
            <label htmlFor={id} className="sr-only">
                {label}
            </label>
            <Input
                id={id}
                placeholder={placeholder}
                value={value}
                onChange={event => onChange(event.target.value)}
                className="h-8 w-64 pl-9"
            />
        </div>
    );
}
