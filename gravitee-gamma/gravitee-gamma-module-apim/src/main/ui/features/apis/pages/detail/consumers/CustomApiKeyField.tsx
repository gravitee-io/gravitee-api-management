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
import { Input, Label } from '@gravitee/graphene-core';

interface CustomApiKeyFieldProps {
    id: string;
    label: string;
    value: string;
    onChange: (value: string) => void;
    error: string | null;
    checking: boolean;
    disabled?: boolean;
    placeholder?: string;
    description?: string;
}

export function CustomApiKeyField({
    id,
    label,
    value,
    onChange,
    error,
    checking,
    disabled,
    placeholder,
    description,
}: Readonly<CustomApiKeyFieldProps>) {
    return (
        <div className="space-y-2">
            <Label htmlFor={id}>{label}</Label>
            <Input
                id={id}
                value={value}
                placeholder={placeholder}
                onChange={event => onChange(event.target.value)}
                disabled={disabled}
                autoComplete="off"
                aria-invalid={error ? true : undefined}
                aria-describedby={error ? `${id}-error` : undefined}
            />
            {description ? <p className="text-xs text-muted-foreground">{description}</p> : null}
            {checking ? <p className="text-xs text-muted-foreground">Checking API key…</p> : null}
            {error ? (
                <p id={`${id}-error`} className="text-xs text-destructive">
                    {error}
                </p>
            ) : null}
        </div>
    );
}
