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

import { useHasFeature } from '@gravitee/gamma-modules-sdk';
import {
    Button,
    Card,
    CardContent,
    Field,
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    RadioGroup,
    RadioGroupItem,
} from '@gravitee/graphene-core';
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { IdentityProviderConfigurationFields, IdentityProviderUserProfileFields } from './IdentityProviderConfigurationFields';
import { IdentityProviderTypeSelector } from './IdentityProviderTypeSelector';
import { RequiredMark } from './RequiredMark';
import { ToggleRow } from './ToggleRow';
import { notify } from '../../../shared/notify';
import { useCreateIdentityProvider } from '../hooks/useIdentityProviderMutations';
import { OPENID_CONNECT_SSO_LICENSE_FEATURE } from '../license/openidConnectSsoLicense';
import { hasUserProfileMapping } from '../utils/identityProviderDisplay';
import {
    emptyIdentityProviderForm,
    formToCreatePayload,
    formWithType,
    IDENTITY_PROVIDER_NAME_MAX,
    IDENTITY_PROVIDER_NAME_MIN,
    validateIdentityProviderForm,
    type IdentityProviderFormState,
} from '../utils/identityProviderForm';

export function IdentityProviderCreateForm() {
    const navigate = useNavigate();
    const hasOpenIdConnectLicense = useHasFeature(OPENID_CONNECT_SSO_LICENSE_FEATURE);
    const createMutation = useCreateIdentityProvider();
    const [form, setForm] = useState<IdentityProviderFormState>(emptyIdentityProviderForm);
    const [showErrors, setShowErrors] = useState(false);

    const errors = validateIdentityProviderForm(form);
    const showProfile = hasUserProfileMapping(form.type);

    function patchConfiguration(patch: Partial<IdentityProviderFormState['configuration']>) {
        setForm(prev => ({ ...prev, configuration: { ...prev.configuration, ...patch } }));
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (Object.keys(errors).length > 0) {
            setShowErrors(true);
            return;
        }
        try {
            await createMutation.mutateAsync(formToCreatePayload(form));
            notify.success('Identity provider successfully saved!');
            navigate('..');
        } catch (error: unknown) {
            notify.error(error, 'Failed to create identity provider');
        }
    }

    return (
        <form className="space-y-6" onSubmit={handleSubmit}>
            <Card>
                <CardContent className="space-y-4 pt-6">
                    <h2 className="text-base font-semibold">Provider type</h2>
                    <IdentityProviderTypeSelector
                        value={form.type}
                        hasOpenIdConnectLicense={hasOpenIdConnectLicense}
                        onChange={type => setForm(prev => (prev.type === type ? prev : formWithType(prev, type)))}
                    />
                </CardContent>
            </Card>

            <Card>
                <CardContent className="space-y-4 pt-6">
                    <h2 className="text-base font-semibold">General</h2>
                    <Field>
                        <FieldLabel htmlFor="idp-name">
                            Name <RequiredMark />
                        </FieldLabel>
                        <Input
                            id="idp-name"
                            value={form.name}
                            minLength={IDENTITY_PROVIDER_NAME_MIN}
                            maxLength={IDENTITY_PROVIDER_NAME_MAX}
                            aria-required="true"
                            aria-invalid={showErrors && !!errors.name}
                            aria-describedby={showErrors && errors.name ? 'idp-name-error' : 'idp-name-hint'}
                            onChange={event => setForm(prev => ({ ...prev, name: event.target.value }))}
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
                            onChange={event => setForm(prev => ({ ...prev, description: event.target.value }))}
                        />
                        <FieldDescription>Provide a description of the identity provider.</FieldDescription>
                    </Field>
                    <ToggleRow
                        id="idp-enabled"
                        label="Allow portal authentication to use this identity provider"
                        checked={form.enabled}
                        disabled={false}
                        onToggle={enabled => setForm(prev => ({ ...prev, enabled }))}
                    />
                    <ToggleRow
                        id="idp-email-required"
                        label="A public email is required to be able to authenticate"
                        checked={form.emailRequired}
                        disabled={false}
                        onToggle={emailRequired => setForm(prev => ({ ...prev, emailRequired }))}
                    />
                    <Field>
                        <FieldLabel>Group and role mappings</FieldLabel>
                        <FieldDescription>Platform administrators still have the ability to override mappings.</FieldDescription>
                        <RadioGroup
                            value={form.syncMappings ? 'each' : 'first'}
                            onValueChange={value => setForm(prev => ({ ...prev, syncMappings: value === 'each' }))}
                            className="mt-2 space-y-2"
                            aria-label="Group and role mappings"
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
                </CardContent>
            </Card>

            <Card>
                <CardContent className="space-y-4 pt-6">
                    <h2 className="text-base font-semibold">Configuration</h2>
                    <IdentityProviderConfigurationFields form={form} showErrors={showErrors} errors={errors} onPatch={patchConfiguration} />
                </CardContent>
            </Card>

            {showProfile ? (
                <Card>
                    <CardContent className="space-y-4 pt-6">
                        <h2 className="text-base font-semibold">User profile mapping</h2>
                        <IdentityProviderUserProfileFields
                            form={form}
                            showErrors={showErrors}
                            errors={errors}
                            onChange={userProfileMapping => setForm(prev => ({ ...prev, userProfileMapping }))}
                        />
                    </CardContent>
                </Card>
            ) : null}

            <div className="sticky bottom-0 z-10 flex flex-wrap items-center justify-end gap-2 border-t bg-background py-4">
                <Button type="button" variant="outline" onClick={() => navigate('..')} disabled={createMutation.isPending}>
                    Cancel
                </Button>
                <Button type="submit" disabled={createMutation.isPending}>
                    {createMutation.isPending ? 'Creating…' : 'Create'}
                </Button>
            </div>
        </form>
    );
}
