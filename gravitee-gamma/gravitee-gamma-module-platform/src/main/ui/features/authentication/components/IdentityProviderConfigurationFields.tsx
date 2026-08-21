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

import { Field, FieldError, FieldLabel, Input, PasswordInput } from '@gravitee/graphene-core';

import { ColorField } from './ColorField';
import { RequiredMark } from './RequiredMark';
import { ChipInput } from '../../shared/components';
import type { IdentityProviderFormState } from '../utils/identityProviderForm';

export function IdentityProviderConfigurationFields({
    form,
    showErrors,
    errors,
    disabled = false,
    onPatch,
}: Readonly<{
    form: IdentityProviderFormState;
    showErrors: boolean;
    errors: Record<string, string>;
    disabled?: boolean;
    onPatch: (patch: Partial<IdentityProviderFormState['configuration']>) => void;
}>) {
    return (
        <div className="space-y-4">
            <Field>
                <FieldLabel htmlFor="idp-client-id">
                    Client Id <RequiredMark />
                </FieldLabel>
                <Input
                    id="idp-client-id"
                    value={form.configuration.clientId}
                    disabled={disabled}
                    aria-required="true"
                    aria-invalid={showErrors && !!errors.clientId}
                    aria-describedby={showErrors && errors.clientId ? 'idp-client-id-error' : undefined}
                    onChange={event => onPatch({ clientId: event.target.value })}
                />
                {showErrors && errors.clientId ? <FieldError id="idp-client-id-error">{errors.clientId}</FieldError> : null}
            </Field>
            <Field>
                <FieldLabel htmlFor="idp-client-secret">
                    Client Secret <RequiredMark />
                </FieldLabel>
                <PasswordInput
                    id="idp-client-secret"
                    autoComplete="off"
                    value={form.configuration.clientSecret}
                    disabled={disabled}
                    aria-required="true"
                    aria-invalid={showErrors && !!errors.clientSecret}
                    aria-describedby={showErrors && errors.clientSecret ? 'idp-client-secret-error' : undefined}
                    onChange={event => onPatch({ clientSecret: event.target.value })}
                />
                {showErrors && errors.clientSecret ? <FieldError id="idp-client-secret-error">{errors.clientSecret}</FieldError> : null}
            </Field>

            {form.type === 'GRAVITEEIO_AM' ? (
                <>
                    <Field>
                        <FieldLabel htmlFor="idp-server-url">
                            Server URL <RequiredMark />
                        </FieldLabel>
                        <Input
                            id="idp-server-url"
                            type="url"
                            value={form.configuration.serverURL ?? ''}
                            disabled={disabled}
                            aria-required="true"
                            aria-invalid={showErrors && !!errors.serverURL}
                            aria-describedby={showErrors && errors.serverURL ? 'idp-server-url-error' : undefined}
                            onChange={event => onPatch({ serverURL: event.target.value })}
                        />
                        {showErrors && errors.serverURL ? <FieldError id="idp-server-url-error">{errors.serverURL}</FieldError> : null}
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="idp-domain">
                            Security domain <RequiredMark />
                        </FieldLabel>
                        <Input
                            id="idp-domain"
                            value={form.configuration.domain ?? ''}
                            disabled={disabled}
                            aria-required="true"
                            aria-invalid={showErrors && !!errors.domain}
                            aria-describedby={showErrors && errors.domain ? 'idp-domain-error' : undefined}
                            onChange={event => onPatch({ domain: event.target.value })}
                        />
                        {showErrors && errors.domain ? <FieldError id="idp-domain-error">{errors.domain}</FieldError> : null}
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="idp-scopes">Scopes</FieldLabel>
                        <ChipInput
                            id="idp-scopes"
                            values={form.configuration.scopes ?? []}
                            placeholder="Enter a scope and press Enter"
                            disabled={disabled}
                            onChange={scopes => onPatch({ scopes })}
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="idp-color">Authentication button color</FieldLabel>
                        <ColorField
                            id="idp-color"
                            value={form.configuration.color ?? ''}
                            disabled={disabled}
                            onChange={color => onPatch({ color })}
                        />
                    </Field>
                </>
            ) : null}

            {form.type === 'OIDC' ? (
                <>
                    <Field>
                        <FieldLabel htmlFor="idp-token-endpoint">
                            Token Endpoint <RequiredMark />
                        </FieldLabel>
                        <Input
                            id="idp-token-endpoint"
                            type="url"
                            value={form.configuration.tokenEndpoint ?? ''}
                            disabled={disabled}
                            aria-required="true"
                            aria-invalid={showErrors && !!errors.tokenEndpoint}
                            aria-describedby={showErrors && errors.tokenEndpoint ? 'idp-token-endpoint-error' : undefined}
                            onChange={event => onPatch({ tokenEndpoint: event.target.value })}
                        />
                        {showErrors && errors.tokenEndpoint ? (
                            <FieldError id="idp-token-endpoint-error">{errors.tokenEndpoint}</FieldError>
                        ) : null}
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="idp-token-introspection">Token Introspection Endpoint</FieldLabel>
                        <Input
                            id="idp-token-introspection"
                            type="url"
                            value={form.configuration.tokenIntrospectionEndpoint ?? ''}
                            disabled={disabled}
                            onChange={event => onPatch({ tokenIntrospectionEndpoint: event.target.value })}
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="idp-authorize-endpoint">
                            Authorize Endpoint <RequiredMark />
                        </FieldLabel>
                        <Input
                            id="idp-authorize-endpoint"
                            type="url"
                            value={form.configuration.authorizeEndpoint ?? ''}
                            disabled={disabled}
                            aria-required="true"
                            aria-invalid={showErrors && !!errors.authorizeEndpoint}
                            aria-describedby={showErrors && errors.authorizeEndpoint ? 'idp-authorize-endpoint-error' : undefined}
                            onChange={event => onPatch({ authorizeEndpoint: event.target.value })}
                        />
                        {showErrors && errors.authorizeEndpoint ? (
                            <FieldError id="idp-authorize-endpoint-error">{errors.authorizeEndpoint}</FieldError>
                        ) : null}
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="idp-userinfo-endpoint">
                            UserInfo Endpoint <RequiredMark />
                        </FieldLabel>
                        <Input
                            id="idp-userinfo-endpoint"
                            type="url"
                            value={form.configuration.userInfoEndpoint ?? ''}
                            disabled={disabled}
                            aria-required="true"
                            aria-invalid={showErrors && !!errors.userInfoEndpoint}
                            aria-describedby={showErrors && errors.userInfoEndpoint ? 'idp-userinfo-endpoint-error' : undefined}
                            onChange={event => onPatch({ userInfoEndpoint: event.target.value })}
                        />
                        {showErrors && errors.userInfoEndpoint ? (
                            <FieldError id="idp-userinfo-endpoint-error">{errors.userInfoEndpoint}</FieldError>
                        ) : null}
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="idp-logout-endpoint">UserInfo Logout Endpoint</FieldLabel>
                        <Input
                            id="idp-logout-endpoint"
                            type="url"
                            value={form.configuration.userLogoutEndpoint ?? ''}
                            disabled={disabled}
                            onChange={event => onPatch({ userLogoutEndpoint: event.target.value })}
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="idp-oidc-scopes">
                            Scopes <RequiredMark />
                        </FieldLabel>
                        <ChipInput
                            id="idp-oidc-scopes"
                            values={form.configuration.scopes ?? []}
                            placeholder={
                                (form.configuration.scopes ?? []).length === 0 ? 'default scopes are openid, profile and email' : ''
                            }
                            required
                            invalid={showErrors && !!errors.scopes}
                            describedBy={showErrors && errors.scopes ? 'idp-oidc-scopes-error' : undefined}
                            disabled={disabled}
                            onChange={scopes => onPatch({ scopes })}
                        />
                        {showErrors && errors.scopes ? <FieldError id="idp-oidc-scopes-error">{errors.scopes}</FieldError> : null}
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="idp-oidc-color">Authentication button color</FieldLabel>
                        <ColorField
                            id="idp-oidc-color"
                            value={form.configuration.color ?? ''}
                            disabled={disabled}
                            onChange={color => onPatch({ color })}
                        />
                    </Field>
                </>
            ) : null}
        </div>
    );
}

export function IdentityProviderUserProfileFields({
    form,
    showErrors,
    errors,
    disabled = false,
    onChange,
}: Readonly<{
    form: IdentityProviderFormState;
    showErrors: boolean;
    errors: Record<string, string>;
    disabled?: boolean;
    onChange: (mapping: IdentityProviderFormState['userProfileMapping']) => void;
}>) {
    return (
        <div className="space-y-4">
            <Field>
                <FieldLabel htmlFor="idp-profile-id">
                    ID <RequiredMark />
                </FieldLabel>
                <Input
                    id="idp-profile-id"
                    value={form.userProfileMapping.id}
                    placeholder="sub"
                    disabled={disabled}
                    aria-required="true"
                    aria-invalid={showErrors && !!errors.profileId}
                    aria-describedby={showErrors && errors.profileId ? 'idp-profile-id-error' : undefined}
                    onChange={event => onChange({ ...form.userProfileMapping, id: event.target.value })}
                />
                {showErrors && errors.profileId ? <FieldError id="idp-profile-id-error">{errors.profileId}</FieldError> : null}
            </Field>
            <Field>
                <FieldLabel htmlFor="idp-profile-firstname">First name</FieldLabel>
                <Input
                    id="idp-profile-firstname"
                    value={form.userProfileMapping.firstname ?? ''}
                    placeholder="given_name"
                    disabled={disabled}
                    onChange={event => onChange({ ...form.userProfileMapping, firstname: event.target.value })}
                />
            </Field>
            <Field>
                <FieldLabel htmlFor="idp-profile-lastname">Last name</FieldLabel>
                <Input
                    id="idp-profile-lastname"
                    value={form.userProfileMapping.lastname ?? ''}
                    placeholder="family_name"
                    disabled={disabled}
                    onChange={event => onChange({ ...form.userProfileMapping, lastname: event.target.value })}
                />
            </Field>
            <Field>
                <FieldLabel htmlFor="idp-profile-email">Email</FieldLabel>
                <Input
                    id="idp-profile-email"
                    value={form.userProfileMapping.email ?? ''}
                    placeholder="email"
                    disabled={disabled}
                    onChange={event => onChange({ ...form.userProfileMapping, email: event.target.value })}
                />
            </Field>
            <Field>
                <FieldLabel htmlFor="idp-profile-picture">Picture</FieldLabel>
                <Input
                    id="idp-profile-picture"
                    value={form.userProfileMapping.picture ?? ''}
                    placeholder="picture"
                    disabled={disabled}
                    onChange={event => onChange({ ...form.userProfileMapping, picture: event.target.value })}
                />
            </Field>
        </div>
    );
}
