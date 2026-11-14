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
import { DatePicker, Field, FieldDescription, FieldError, FieldLabel } from '@gravitee/graphene-core';

import { toDateInput } from '../utils/credentialDraft';

export function CredentialExpiryField({
    id,
    value,
    error,
    onChange,
}: Readonly<{ id: string; value: string; error?: string; onChange: (value: string) => void }>) {
    return (
        <Field orientation="vertical" className="gap-1.5">
            <FieldLabel htmlFor={id}>Expires (optional)</FieldLabel>
            <DatePicker
                id={id}
                placeholder="Never expires"
                value={value ? new Date(`${value}T00:00:00`) : undefined}
                onChange={date => onChange(date ? toDateInput(date) : '')}
            />
            <FieldDescription>A reminder to replace the credential. It does not stop the credential from being used.</FieldDescription>
            {error ? <FieldError>{error}</FieldError> : null}
        </Field>
    );
}
