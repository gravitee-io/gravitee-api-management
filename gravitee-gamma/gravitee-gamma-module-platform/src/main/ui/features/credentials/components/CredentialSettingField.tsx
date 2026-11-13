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
import { Field, FieldDescription, FieldError, FieldLabel, Input } from '@gravitee/graphene-core';

import type { CredentialField } from '../utils/credentialStrategies';

export function CredentialSettingField({
    id,
    field,
    value,
    error,
    onChange,
}: Readonly<{ id: string; field: CredentialField; value: string; error?: string; onChange: (value: string) => void }>) {
    const descriptionId = `${id}-description`;
    const errorId = `${id}-error`;
    const describedBy = [field.list ? descriptionId : undefined, error ? errorId : undefined].filter(Boolean).join(' ');
    return (
        <Field orientation="vertical" className="gap-1.5">
            <FieldLabel htmlFor={id} required={!field.optional}>
                {field.label}
            </FieldLabel>
            <Input
                id={id}
                value={value}
                placeholder={field.placeholder}
                aria-invalid={Boolean(error)}
                aria-describedby={describedBy || undefined}
                onChange={e => onChange(e.target.value)}
            />
            {field.list ? <FieldDescription id={descriptionId}>Separate several values with commas.</FieldDescription> : null}
            {error ? <FieldError id={errorId}>{error}</FieldError> : null}
        </Field>
    );
}
