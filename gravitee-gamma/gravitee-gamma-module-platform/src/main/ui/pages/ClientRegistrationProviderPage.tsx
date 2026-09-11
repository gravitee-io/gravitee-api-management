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
import {
    Alert,
    AlertDescription,
    Badge,
    Button,
    Card,
    CardContent,
    Field,
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    PasswordInput,
    RadioGroup,
    RadioGroupItem,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Skeleton,
    Switch,
    Textarea,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon, TriangleAlertIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { ClaimMappingsFields } from '../features/client-registration/components/ClaimMappingsFields';
import {
    useCreateClientRegistrationProvider,
    useUpdateClientRegistrationProvider,
} from '../features/client-registration/hooks/useClientRegistrationMutations';
import { useClientRegistrationPermissions } from '../features/client-registration/hooks/useClientRegistrationPermissions';
import { useClientRegistrationProvider } from '../features/client-registration/hooks/useClientRegistrationProvider';
import type {
    InitialAccessTokenType,
    RenewClientSecretMethod,
    StoreType,
} from '../features/client-registration/types/clientRegistrationProvider';
import { EMPTY_PROVIDER_FORM, formToWrite, providerToForm } from '../features/client-registration/utils/providerForm';
import {
    PROVIDER_NAME_MAX,
    validateProviderForm,
    type PathOrContent,
    type StoreForm,
} from '../features/client-registration/utils/validateProviderForm';
import { notify } from '../shared/notify';

const RENEW_METHODS: RenewClientSecretMethod[] = ['POST', 'PATCH', 'PUT'];
const STORE_TYPES: { value: StoreType; label: string }[] = [
    { value: 'NONE', label: 'None' },
    { value: 'JKS', label: 'Java Trust Store (.jks)' },
    { value: 'PKCS12', label: 'PKCS#12 (.p12) / PFX (.pfx)' },
];
const KEY_STORE_TYPES: { value: StoreType; label: string }[] = [
    { value: 'NONE', label: 'None' },
    { value: 'JKS', label: 'Java Key Store (.jks)' },
    { value: 'PKCS12', label: 'PKCS#12 (.p12) / PFX (.pfx)' },
];
const RENEW_ENDPOINT_EXAMPLE = 'https://[am_gateway]/[domain]/oidc/register/{#client_id}/renew_secret';
const TOKEN_TYPE_UNSET = '__unset_token_type__';
const RENEW_METHOD_UNSET = '__unset_renew_method__';

function ScopesChipInput({
    values,
    onChange,
    disabled,
}: Readonly<{
    values: string[];
    onChange: (next: string[]) => void;
    disabled: boolean;
}>) {
    const [draft, setDraft] = useState('');

    function commit() {
        const next = draft.trim();
        if (!next || values.includes(next)) {
            setDraft('');
            return;
        }
        onChange([...values, next]);
        setDraft('');
    }

    return (
        <div className="flex flex-wrap items-center gap-1.5 rounded-md border border-input bg-transparent px-2 py-1.5">
            {values.map(value => (
                <Badge key={value} variant="secondary" className="gap-1 font-normal">
                    {value}
                    {disabled ? null : (
                        <button
                            type="button"
                            className="ml-0.5"
                            aria-label={`Remove ${value}`}
                            onClick={() => onChange(values.filter(item => item !== value))}
                        >
                            <XIcon className="size-3" aria-hidden />
                        </button>
                    )}
                </Badge>
            ))}
            <Input
                value={draft}
                disabled={disabled}
                placeholder={values.length === 0 ? 'Enter a scope and press Enter' : ''}
                className="flex-1 border-0 px-1 shadow-none focus-visible:ring-0"
                style={{ height: 28, minWidth: 160 }}
                onChange={event => setDraft(event.target.value)}
                onKeyDown={event => {
                    if (event.key === 'Enter' || event.key === ',') {
                        event.preventDefault();
                        commit();
                    }
                }}
                onBlur={commit}
            />
        </div>
    );
}

function StoreFields({
    idPrefix,
    store,
    types,
    errors,
    showErrors,
    disabled,
    includeKeyFields,
    onChange,
}: Readonly<{
    idPrefix: string;
    store: StoreForm;
    types: { value: StoreType; label: string }[];
    errors: Record<string, string>;
    showErrors: boolean;
    disabled: boolean;
    includeKeyFields: boolean;
    onChange: (next: StoreForm) => void;
}>) {
    const passwordError = includeKeyFields ? errors.key_password : errors.trust_password;
    const pathError = includeKeyFields ? errors.key_path : errors.trust_path;
    const contentError = includeKeyFields ? errors.key_content : errors.trust_content;
    const kind = store.type === 'PKCS12' ? 'PKCS12' : 'JKS';

    return (
        <div className="space-y-4">
            <Field>
                <FieldLabel htmlFor={`${idPrefix}-type`}>{includeKeyFields ? 'Key Store Type' : 'Trust Store Type'}</FieldLabel>
                <Select value={store.type} onValueChange={type => onChange({ ...store, type: type as StoreType })} disabled={disabled}>
                    <SelectTrigger id={`${idPrefix}-type`}>
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        {types.map(type => (
                            <SelectItem key={type.value} value={type.value}>
                                {type.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </Field>

            {store.type === 'NONE' ? null : (
                <>
                    <Field>
                        <FieldLabel>Input</FieldLabel>
                        <RadioGroup
                            value={store.pathOrContent}
                            onValueChange={value => onChange({ ...store, pathOrContent: value as PathOrContent })}
                            disabled={disabled}
                            className="flex flex-row gap-4"
                        >
                            <div className="flex items-center gap-2 text-sm">
                                <RadioGroupItem value="PATH" id={`${idPrefix}-path-or-content-path`} />
                                <FieldLabel htmlFor={`${idPrefix}-path-or-content-path`} className="cursor-pointer font-normal">
                                    Path
                                </FieldLabel>
                            </div>
                            <div className="flex items-center gap-2 text-sm">
                                <RadioGroupItem value="CONTENT" id={`${idPrefix}-path-or-content-content`} />
                                <FieldLabel htmlFor={`${idPrefix}-path-or-content-content`} className="cursor-pointer font-normal">
                                    Content (Base64)
                                </FieldLabel>
                            </div>
                        </RadioGroup>
                    </Field>

                    <Field>
                        <FieldLabel htmlFor={`${idPrefix}-password`} required>
                            {kind} Password
                        </FieldLabel>
                        <Input
                            id={`${idPrefix}-password`}
                            type="password"
                            autoComplete="off"
                            value={disabled && store.password ? '********' : store.password}
                            disabled={disabled}
                            aria-invalid={showErrors && passwordError ? true : undefined}
                            onChange={event => onChange({ ...store, password: event.target.value })}
                        />
                        {showErrors && passwordError ? <FieldError>{passwordError}</FieldError> : null}
                    </Field>

                    {store.pathOrContent === 'PATH' ? (
                        <Field>
                            <FieldLabel htmlFor={`${idPrefix}-path`}>{kind} Path</FieldLabel>
                            <Input
                                id={`${idPrefix}-path`}
                                value={store.path}
                                disabled={disabled}
                                aria-invalid={showErrors && pathError ? true : undefined}
                                onChange={event => onChange({ ...store, path: event.target.value })}
                            />
                            <FieldDescription>
                                Path to the {includeKeyFields ? 'key' : 'trust'} store file
                                {store.type === 'JKS' ? ' (.jks)' : ' (.p12)'}
                            </FieldDescription>
                            {showErrors && pathError ? <FieldError>{pathError}</FieldError> : null}
                        </Field>
                    ) : (
                        <Field>
                            <FieldLabel htmlFor={`${idPrefix}-content`}>{kind} Content (Base64)</FieldLabel>
                            <Textarea
                                id={`${idPrefix}-content`}
                                value={store.content}
                                disabled={disabled}
                                rows={2}
                                aria-invalid={showErrors && contentError ? true : undefined}
                                onChange={event => onChange({ ...store, content: event.target.value })}
                            />
                            <FieldDescription>Binary content as Base64</FieldDescription>
                            {showErrors && contentError ? <FieldError>{contentError}</FieldError> : null}
                        </Field>
                    )}

                    {includeKeyFields ? (
                        <>
                            <Field>
                                <FieldLabel htmlFor={`${idPrefix}-alias`}>Alias</FieldLabel>
                                <Input
                                    id={`${idPrefix}-alias`}
                                    value={store.alias}
                                    disabled={disabled}
                                    onChange={event => onChange({ ...store, alias: event.target.value })}
                                />
                                <FieldDescription>Alias of the key to use</FieldDescription>
                            </Field>
                            <Field>
                                <FieldLabel htmlFor={`${idPrefix}-key-password`}>Key Password</FieldLabel>
                                <Input
                                    id={`${idPrefix}-key-password`}
                                    type="password"
                                    autoComplete="off"
                                    value={disabled && store.keyPassword ? '********' : store.keyPassword}
                                    disabled={disabled}
                                    onChange={event => onChange({ ...store, keyPassword: event.target.value })}
                                />
                                <FieldDescription>Password protecting the individual key</FieldDescription>
                            </Field>
                        </>
                    ) : null}
                </>
            )}
        </div>
    );
}

export function ClientRegistrationProviderPage() {
    const { providerId } = useParams<{ providerId: string }>();
    const isUpdate = providerId !== undefined;
    const navigate = useNavigate();
    const { data: provider, isLoading, isError, refetch } = useClientRegistrationProvider(providerId);
    const { canSaveProvider } = useClientRegistrationPermissions();
    const createMutation = useCreateClientRegistrationProvider();
    const updateMutation = useUpdateClientRegistrationProvider();

    const [form, setForm] = useState(() => (provider ? providerToForm(provider) : EMPTY_PROVIDER_FORM));
    const [showErrors, setShowErrors] = useState(false);
    const seededProviderId = useRef<string | undefined>(provider?.id);

    useEffect(() => {
        if (!isUpdate) {
            seededProviderId.current = undefined;
            setForm(EMPTY_PROVIDER_FORM);
            return;
        }
        if (provider && seededProviderId.current !== provider.id) {
            seededProviderId.current = provider.id;
            setForm(providerToForm(provider));
        }
    }, [isUpdate, provider]);

    const errors = useMemo(() => validateProviderForm(form), [form]);
    const isSaving = createMutation.isPending || updateMutation.isPending;
    const readOnly = !canSaveProvider;

    function handleSubmit(event: FormEvent) {
        event.preventDefault();
        const nextErrors = validateProviderForm(form);
        if (Object.keys(nextErrors).length > 0) {
            setShowErrors(true);
            return;
        }
        const write = formToWrite(form);
        const onSuccess = (saved: { id: string; name: string }) => {
            notify.success(
                isUpdate
                    ? `Client registration provider ${saved.name} has been updated.`
                    : `Client registration provider ${saved.name} has been created.`,
            );
            navigate(`../${saved.id}`, { replace: true });
        };
        if (isUpdate && providerId) {
            updateMutation.mutate(
                { providerId, provider: write },
                {
                    onSuccess,
                    onError: error => notify.error(error, 'Failed to save provider'),
                },
            );
        } else {
            createMutation.mutate(write, {
                onSuccess,
                onError: error => notify.error(error, 'Failed to save provider'),
            });
        }
    }

    if (isUpdate && isError) {
        return (
            <div className="space-y-6">
                <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" asChild>
                    <Link to="..">
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to Client Registration
                    </Link>
                </Button>
                <Alert variant="destructive">
                    <TriangleAlertIcon className="size-4" aria-hidden />
                    <AlertDescription className="flex flex-wrap items-center gap-3">
                        Could not load this provider.
                        <Button size="sm" variant="outline" onClick={() => void refetch()}>
                            Try again
                        </Button>
                    </AlertDescription>
                </Alert>
            </div>
        );
    }

    if (isUpdate && isLoading) {
        return (
            <div className="space-y-6">
                <Skeleton className="w-48 rounded-lg" style={{ height: 28 }} />
                <Skeleton className="w-full rounded-xl" style={{ height: 220 }} />
                <Skeleton className="w-full rounded-xl" style={{ height: 320 }} />
            </div>
        );
    }

    return (
        <form className="space-y-6" onSubmit={handleSubmit} noValidate>
            <div className="space-y-2">
                <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" asChild>
                    <Link to="..">
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to Client Registration
                    </Link>
                </Button>
                <h1 className="text-2xl font-semibold tracking-tight">
                    {isUpdate ? 'Update client registration provider' : 'New client registration provider'}
                </h1>
            </div>

            <Card>
                <CardContent className="space-y-4 pt-6">
                    <h2 className="text-base font-semibold">General</h2>
                    <Field>
                        <FieldLabel htmlFor="dcr-name" required>
                            Name
                        </FieldLabel>
                        <Input
                            id="dcr-name"
                            value={form.name}
                            maxLength={PROVIDER_NAME_MAX}
                            disabled={readOnly}
                            aria-invalid={showErrors && errors.name ? true : undefined}
                            onChange={event => setForm(prev => ({ ...prev, name: event.target.value }))}
                        />
                        <FieldDescription>Client registration provider name.</FieldDescription>
                        {showErrors && errors.name ? <FieldError>{errors.name}</FieldError> : null}
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="dcr-description">Description</FieldLabel>
                        <Input
                            id="dcr-description"
                            value={form.description}
                            disabled={readOnly}
                            onChange={event => setForm(prev => ({ ...prev, description: event.target.value }))}
                        />
                        <FieldDescription>Provide a description to the client registration provider.</FieldDescription>
                    </Field>
                </CardContent>
            </Card>

            <Card>
                <CardContent className="space-y-4 pt-6">
                    <div className="space-y-1">
                        <h2 className="text-base font-semibold">Configuration</h2>
                        <p className="text-sm font-medium">OpenID Connect - Dynamic Client Registration</p>
                    </div>

                    <Field>
                        <FieldLabel htmlFor="dcr-discovery" required>
                            OpenID Connect Discovery Endpoint
                        </FieldLabel>
                        <Input
                            id="dcr-discovery"
                            type="url"
                            value={form.discovery_endpoint}
                            disabled={readOnly}
                            aria-invalid={showErrors && errors.discovery_endpoint ? true : undefined}
                            onChange={event => setForm(prev => ({ ...prev, discovery_endpoint: event.target.value }))}
                        />
                        {showErrors && errors.discovery_endpoint ? <FieldError>{errors.discovery_endpoint}</FieldError> : null}
                    </Field>

                    <Field>
                        <FieldLabel htmlFor="dcr-token-type" required>
                            Initial Access Token Provider
                        </FieldLabel>
                        <Select
                            value={form.initial_access_token_type || TOKEN_TYPE_UNSET}
                            onValueChange={value =>
                                setForm(prev => ({
                                    ...prev,
                                    initial_access_token_type: value === TOKEN_TYPE_UNSET ? '' : (value as InitialAccessTokenType),
                                }))
                            }
                            disabled={readOnly}
                        >
                            <SelectTrigger id="dcr-token-type">
                                <SelectValue placeholder="Select how the initial access token is provided" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value={TOKEN_TYPE_UNSET} disabled>
                                    Select how the initial access token is provided
                                </SelectItem>
                                <SelectItem value="CLIENT_CREDENTIALS">Client Credentials</SelectItem>
                                <SelectItem value="INITIAL_ACCESS_TOKEN">Initial Access Token</SelectItem>
                            </SelectContent>
                        </Select>
                        <FieldDescription>Define the way the initial access token must be provided.</FieldDescription>
                        {showErrors && errors.initial_access_token_type ? (
                            <FieldError>{errors.initial_access_token_type}</FieldError>
                        ) : null}
                    </Field>

                    {form.initial_access_token_type === 'CLIENT_CREDENTIALS' ? (
                        <>
                            <Field>
                                <FieldLabel htmlFor="dcr-client-id" required>
                                    Client ID
                                </FieldLabel>
                                <Input
                                    id="dcr-client-id"
                                    value={form.client_id}
                                    disabled={readOnly}
                                    aria-invalid={showErrors && errors.client_id ? true : undefined}
                                    onChange={event => setForm(prev => ({ ...prev, client_id: event.target.value }))}
                                />
                                {showErrors && errors.client_id ? <FieldError>{errors.client_id}</FieldError> : null}
                            </Field>
                            <Field>
                                <FieldLabel htmlFor="dcr-client-secret" required>
                                    Client Secret
                                </FieldLabel>
                                {readOnly ? (
                                    <Input
                                        id="dcr-client-secret"
                                        type="password"
                                        autoComplete="off"
                                        value={form.client_secret ? '********' : ''}
                                        disabled
                                        aria-invalid={showErrors && errors.client_secret ? true : undefined}
                                    />
                                ) : (
                                    <PasswordInput
                                        id="dcr-client-secret"
                                        autoComplete="off"
                                        value={form.client_secret}
                                        aria-invalid={showErrors && errors.client_secret ? true : undefined}
                                        onChange={event => setForm(prev => ({ ...prev, client_secret: event.target.value }))}
                                    />
                                )}
                                {showErrors && errors.client_secret ? <FieldError>{errors.client_secret}</FieldError> : null}
                            </Field>
                            <Field>
                                <FieldLabel>Scopes</FieldLabel>
                                <ScopesChipInput
                                    values={form.scopes}
                                    disabled={readOnly}
                                    onChange={scopes => setForm(prev => ({ ...prev, scopes }))}
                                />
                            </Field>
                            <Field>
                                <FieldLabel htmlFor="dcr-software-id">Client Template (software_id)</FieldLabel>
                                <Input
                                    id="dcr-software-id"
                                    value={form.software_id}
                                    disabled={readOnly}
                                    onChange={event => setForm(prev => ({ ...prev, software_id: event.target.value }))}
                                />
                            </Field>
                        </>
                    ) : null}

                    {form.initial_access_token_type === 'INITIAL_ACCESS_TOKEN' ? (
                        <Field>
                            <FieldLabel htmlFor="dcr-iat" required>
                                Initial Access Token
                            </FieldLabel>
                            <Input
                                id="dcr-iat"
                                value={form.initial_access_token}
                                disabled={readOnly}
                                aria-invalid={showErrors && errors.initial_access_token ? true : undefined}
                                onChange={event => setForm(prev => ({ ...prev, initial_access_token: event.target.value }))}
                            />
                            {showErrors && errors.initial_access_token ? <FieldError>{errors.initial_access_token}</FieldError> : null}
                        </Field>
                    ) : null}

                    <div className="space-y-4 rounded-xl border p-4">
                        <p className="text-sm font-medium">Trust Store Configuration</p>
                        <StoreFields
                            idPrefix="trust"
                            store={form.trust_store}
                            types={STORE_TYPES}
                            errors={errors}
                            showErrors={showErrors}
                            disabled={readOnly}
                            includeKeyFields={false}
                            onChange={trust_store => setForm(prev => ({ ...prev, trust_store }))}
                        />
                        <p className="text-sm font-medium">Key Store Configuration</p>
                        <StoreFields
                            idPrefix="key"
                            store={form.key_store}
                            types={KEY_STORE_TYPES}
                            errors={errors}
                            showErrors={showErrors}
                            disabled={readOnly}
                            includeKeyFields
                            onChange={key_store => setForm(prev => ({ ...prev, key_store }))}
                        />
                    </div>

                    <div className="space-y-4">
                        <h3 className="text-sm font-semibold">Renew client_secret (outside DCR specification)</h3>
                        <Field orientation="horizontal">
                            <div className="min-w-0 flex-1 space-y-1">
                                <FieldLabel htmlFor="dcr-renew">Enable renew client_secret support</FieldLabel>
                            </div>
                            <Switch
                                id="dcr-renew"
                                checked={form.renew_client_secret_support}
                                disabled={readOnly}
                                aria-label="Renew client secret support"
                                onCheckedChange={renew_client_secret_support => setForm(prev => ({ ...prev, renew_client_secret_support }))}
                            />
                        </Field>
                        <Field>
                            <FieldLabel htmlFor="dcr-renew-method">HTTP Method</FieldLabel>
                            <Select
                                value={form.renew_client_secret_method || RENEW_METHOD_UNSET}
                                onValueChange={value =>
                                    setForm(prev => ({
                                        ...prev,
                                        renew_client_secret_method: value === RENEW_METHOD_UNSET ? '' : (value as RenewClientSecretMethod),
                                    }))
                                }
                                disabled={readOnly}
                            >
                                <SelectTrigger id="dcr-renew-method">
                                    <SelectValue placeholder="Select a method" />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value={RENEW_METHOD_UNSET} disabled>
                                        Select a method
                                    </SelectItem>
                                    {RENEW_METHODS.map(method => (
                                        <SelectItem key={method} value={method}>
                                            {method}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <FieldDescription>Define the HTTP method to call the renew secret endpoint.</FieldDescription>
                        </Field>
                        <Field>
                            <FieldLabel htmlFor="dcr-renew-endpoint">Endpoint</FieldLabel>
                            <Input
                                id="dcr-renew-endpoint"
                                type="url"
                                value={form.renew_client_secret_endpoint}
                                disabled={readOnly}
                                placeholder={RENEW_ENDPOINT_EXAMPLE}
                                onChange={event => setForm(prev => ({ ...prev, renew_client_secret_endpoint: event.target.value }))}
                            />
                            <FieldDescription>
                                Provide the URL for the renew client secret endpoint. Use the variable{' '}
                                <span className="font-mono text-xs">{'{#client_id}'}</span> — APIM will resolve it at runtime. Example for
                                AM: {RENEW_ENDPOINT_EXAMPLE}
                            </FieldDescription>
                        </Field>
                    </div>
                </CardContent>
            </Card>

            <Card>
                <CardContent className="space-y-4 pt-6">
                    <ClaimMappingsFields
                        rows={form.claim_mappings}
                        disabled={readOnly}
                        error={errors.claim_mappings}
                        showErrors={showErrors}
                        onChange={claim_mappings => setForm(prev => ({ ...prev, claim_mappings }))}
                    />
                </CardContent>
            </Card>

            {canSaveProvider ? (
                <div className="sticky bottom-0 z-10 flex flex-wrap items-center justify-end gap-2 border-t bg-background py-4">
                    <Button type="button" variant="outline" onClick={() => navigate('..')} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" disabled={isSaving}>
                        {isSaving ? 'Saving…' : isUpdate ? 'Save changes' : 'Create provider'}
                    </Button>
                </div>
            ) : null}
        </form>
    );
}
