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

import type { CredentialDraft, CredentialDraftErrors } from '../utils/credentialDraft';

const NAME_ID = 'credential-name';
const NAME_ERROR_ID = 'credential-name-error';
const SERVICE_ID = 'credential-service';
const SERVICE_DESCRIPTION_ID = 'credential-service-description';

export function CredentialNameAndServiceFields({
    draft,
    errors,
    onChange,
}: Readonly<{ draft: CredentialDraft; errors: CredentialDraftErrors; onChange: (draft: CredentialDraft) => void }>) {
    return (
        <>
            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor={NAME_ID} required>
                    Name
                </FieldLabel>
                <Input
                    id={NAME_ID}
                    value={draft.name}
                    maxLength={256}
                    placeholder="OpenAI production"
                    aria-invalid={Boolean(errors.name)}
                    aria-describedby={errors.name ? NAME_ERROR_ID : undefined}
                    onChange={e => onChange({ ...draft, name: e.target.value })}
                />
                {errors.name ? <FieldError id={NAME_ERROR_ID}>{errors.name}</FieldError> : null}
            </Field>
            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor={SERVICE_ID}>Service</FieldLabel>
                <Input
                    id={SERVICE_ID}
                    value={draft.serviceId}
                    placeholder="openai"
                    aria-describedby={SERVICE_DESCRIPTION_ID}
                    onChange={e => onChange({ ...draft, serviceId: e.target.value })}
                />
                <FieldDescription id={SERVICE_DESCRIPTION_ID}>
                    The service the credential calls. Modules use it to find the credential.
                </FieldDescription>
            </Field>
        </>
    );
}
