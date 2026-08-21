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

import { Field, FieldContent, FieldDescription, FieldLabel, Switch } from '@gravitee/graphene-core';

export function ToggleRow({
    id,
    label,
    checked,
    disabled,
    description,
    onToggle,
}: Readonly<{
    id: string;
    label: string;
    checked: boolean;
    disabled: boolean;
    description?: string;
    onToggle: (checked: boolean) => void;
}>) {
    const descriptionId = description ? `${id}-hint` : undefined;

    return (
        <Field orientation="horizontal">
            <FieldContent>
                <FieldLabel htmlFor={id}>{label}</FieldLabel>
                {description ? <FieldDescription id={descriptionId}>{description}</FieldDescription> : null}
            </FieldContent>
            <Switch
                id={id}
                checked={checked}
                disabled={disabled}
                aria-label={label}
                aria-describedby={descriptionId}
                onCheckedChange={next => {
                    if (typeof next === 'boolean') onToggle(next);
                }}
            />
        </Field>
    );
}
