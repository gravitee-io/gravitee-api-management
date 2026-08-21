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

import { Field, FieldDescription, FieldError, FieldLabel, Input, RadioGroup, RadioGroupItem } from '@gravitee/graphene-core';

import { RequiredMark } from './RequiredMark';
import { ToggleRow } from './ToggleRow';
import { IDENTITY_PROVIDER_NAME_MAX, IDENTITY_PROVIDER_NAME_MIN, type IdentityProviderFormState } from '../utils/identityProviderForm';

export function IdentityProviderGeneralFields({
    form,
    showErrors,
    errors,
    disabled,
    onChange,
}: Readonly<{
    form: IdentityProviderFormState;
    showErrors: boolean;
    errors: Record<string, string>;
    disabled: boolean;
    onChange: (patch: Partial<IdentityProviderFormState>) => void;
}>) {
    return (
        <>
            <Field>
                <FieldLabel htmlFor="idp-name">
                    Name <RequiredMark />
                </FieldLabel>
                <Input
                    id="idp-name"
                    value={form.name}
                    minLength={IDENTITY_PROVIDER_NAME_MIN}
                    maxLength={IDENTITY_PROVIDER_NAME_MAX}
                    disabled={disabled}
                    aria-required="true"
                    aria-invalid={showErrors && !!errors.name}
                    aria-describedby={showErrors && errors.name ? 'idp-name-error' : 'idp-name-hint'}
                    onChange={event => onChange({ name: event.target.value })}
                />
                <FieldDescription id="idp-name-hint">
                    Identity provider name. The name will be used to define the authentication endpoint.
                </FieldDescription>
                {showErrors && errors.name ? <FieldError id="idp-name-error">{errors.name}</FieldError> : null}
            </Field>
            <Field>
                <FieldLabel htmlFor="idp-description">Description</FieldLabel>
                <Input
                    id="idp-description"
                    value={form.description}
                    disabled={disabled}
                    onChange={event => onChange({ description: event.target.value })}
                />
                <FieldDescription>Provide a description of the identity provider.</FieldDescription>
            </Field>
            <ToggleRow
                id="idp-enabled"
                label="Allow portal authentication to use this identity provider"
                checked={form.enabled}
                disabled={disabled}
                onToggle={enabled => onChange({ enabled })}
            />
            <ToggleRow
                id="idp-email-required"
                label="A public email is required to be able to authenticate"
                checked={form.emailRequired}
                disabled={disabled}
                onToggle={emailRequired => onChange({ emailRequired })}
            />
            <Field>
                <FieldLabel>Group and role mappings</FieldLabel>
                <FieldDescription>Platform administrators still have the ability to override mappings.</FieldDescription>
                <RadioGroup
                    value={form.syncMappings ? 'each' : 'first'}
                    onValueChange={value => onChange({ syncMappings: value === 'each' })}
                    className="mt-2 space-y-2"
                    aria-label="Group and role mappings"
                    disabled={disabled}
                >
                    <div className="flex items-center gap-2 text-sm">
                        <RadioGroupItem value="first" id="idp-sync-first" />
                        <FieldLabel htmlFor="idp-sync-first" className="cursor-pointer font-normal">
                            Computed only during first user authentication
                        </FieldLabel>
                    </div>
                    <div className="flex items-center gap-2 text-sm">
                        <RadioGroupItem value="each" id="idp-sync-each" />
                        <FieldLabel htmlFor="idp-sync-each" className="cursor-pointer font-normal">
                            Computed during each user authentication
                        </FieldLabel>
                    </div>
                </RadioGroup>
            </Field>
        </>
    );
}
