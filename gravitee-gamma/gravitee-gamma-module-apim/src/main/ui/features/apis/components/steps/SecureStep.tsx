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
    Badge,
    Field,
    FieldContent,
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { CircleCheckIcon, KeyRoundIcon, LockIcon, ShieldCheckIcon } from '@gravitee/graphene-core/icons';
import { SparklesIcon } from 'lucide-react';
import { useMemo } from 'react';
import type React from 'react';

import { usePolicySchema } from '../../hooks/usePolicySchema';
import type { ApiCreationState } from '../../types/models';
import { type SchemaSelectResult, readSchemaSelect } from '../../utils/policySchemaUtils';

type AuthTypeValue = 'keyless' | 'api-key' | 'jwt' | 'oauth2' | 'mtls';

type AuthTypeDefinition = {
    readonly value: AuthTypeValue;
    readonly label: string;
    readonly description: string;
    readonly icon: React.ComponentType<React.SVGProps<SVGSVGElement>>;
    readonly iconColorClass: string;
    readonly showDefaultBadge?: boolean;
};

const AUTH_TYPE_DEFINITIONS: readonly AuthTypeDefinition[] = [
    {
        value: 'keyless',
        label: 'Keyless (Open)',
        description: 'No authentication required. Any consumer can call this API.',
        icon: SparklesIcon,
        iconColorClass: 'text-success',
        showDefaultBadge: true,
    },
    {
        value: 'api-key',
        label: 'API Key',
        description: 'Consumers must include a valid API key in requests.',
        icon: KeyRoundIcon,
        iconColorClass: 'text-blue-500',
    },
    {
        value: 'jwt',
        label: 'JWT',
        description: 'Validate JSON Web Tokens from your identity provider.',
        icon: ShieldCheckIcon,
        iconColorClass: 'text-purple-500',
    },
    {
        value: 'oauth2',
        label: 'OAuth 2.0',
        description: 'Enforce OAuth 2.0 access tokens for enterprise security.',
        icon: LockIcon,
        iconColorClass: 'text-amber-500',
    },
    {
        value: 'mtls',
        label: 'mTLS',
        description: 'Mutual TLS based on the client X.509 certificate.',
        icon: ShieldCheckIcon,
        iconColorClass: 'text-rose-500',
    },
];

function AuthTypeSelector({ value, onChange }: Readonly<{ value: AuthTypeValue; onChange: (v: AuthTypeValue) => void }>) {
    return (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {AUTH_TYPE_DEFINITIONS.map(option => {
                const isSelected = option.value === value;
                const Icon = option.icon;
                return (
                    <button
                        key={option.value}
                        type="button"
                        aria-pressed={isSelected}
                        onClick={() => onChange(option.value)}
                        className={`flex h-full w-full items-start gap-3 rounded-xl border-2 p-4 text-left transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
                            isSelected
                                ? 'border-primary bg-primary/5 ring-1 ring-primary/20'
                                : 'border-border hover:border-foreground/20 hover:shadow-sm'
                        }`}
                    >
                        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-muted">
                            <Icon className={`size-5 ${option.iconColorClass}`} aria-hidden="true" />
                        </div>
                        <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-2">
                                <span className="text-sm font-medium">{option.label}</span>
                                {option.showDefaultBadge ? (
                                    <Badge variant="secondary" className="px-2 py-0.5 text-[10px]">
                                        Default
                                    </Badge>
                                ) : null}
                            </div>
                            <p className="mt-1 text-xs text-muted-foreground">{option.description}</p>
                        </div>
                        {isSelected ? <CircleCheckIcon className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden="true" /> : null}
                    </button>
                );
            })}
        </div>
    );
}

const OAUTH2_FALLBACK_OPTIONS: readonly { value: string; label: string }[] = [
    { value: 'Generic OAuth2 Resource', label: 'Generic OAuth2 Resource' },
    { value: 'Gravitee Access Management', label: 'Gravitee Access Management' },
    { value: 'Keycloak Adapter', label: 'Keycloak Adapter' },
];

type SecureStepProps = Readonly<{
    security: ApiCreationState['security'];
    errors: Record<string, string>;
    getValue: (path: string) => unknown;
    updateField: (path: string, value: unknown) => void;
}>;

export function SecureStep({ security, errors, getValue, updateField }: SecureStepProps) {
    const authType = security.type as AuthTypeValue;
    const jwtSchema = usePolicySchema('jwt', authType === 'jwt');
    const oauth2Schema = usePolicySchema('oauth2', authType === 'oauth2');

    const jwtSignatureOptions = useMemo(() => readSchemaSelect(jwtSchema.data, 'signature'), [jwtSchema.data]);
    const jwtResolverOptionsRaw = useMemo(() => readSchemaSelect(jwtSchema.data, 'publicKeyResolver'), [jwtSchema.data]);
    const jwtResolverOptions = useMemo<SchemaSelectResult>(() => {
        if (!jwtResolverOptionsRaw) return null;
        return {
            ...jwtResolverOptionsRaw,
            options: jwtResolverOptionsRaw.options.map(opt => {
                if (opt.value === 'GIVEN_KEY') return { value: opt.value, label: 'Given key (PEM, single key)' };
                if (opt.value === 'GATEWAY_KEYS') return { value: opt.value, label: 'Gateway keys (configured globally)' };
                if (opt.value === 'JWKS_URL') return { value: opt.value, label: 'JWKS URL' };
                return opt;
            }),
        };
    }, [jwtResolverOptionsRaw]);
    const oauth2ResourceOptions = useMemo(
        () => readSchemaSelect(oauth2Schema.data, 'oauthResource') ?? readSchemaSelect(oauth2Schema.data, 'resource'),
        [oauth2Schema.data],
    );

    const onChangeAuthType = (next: AuthTypeValue) => {
        updateField('security.type', next);
        if (next === 'api-key') {
            const current = getValue('security.planName');
            if (typeof current !== 'string' || current.trim() === '') updateField('security.planName', 'Default API Key plan');
        }
        if (next === 'jwt') {
            const current = getValue('security.planName');
            if (typeof current !== 'string' || current.trim() === '') updateField('security.planName', 'Default JWT plan');
            const sig = getValue('security.signature');
            if (typeof sig !== 'string' || sig.trim() === '')
                updateField('security.signature', jwtSignatureOptions?.defaultValue ?? 'RSA_RS256');
            const resolver = getValue('security.jwksResolver');
            if (typeof resolver !== 'string' || resolver.trim() === '')
                updateField('security.jwksResolver', jwtResolverOptions?.defaultValue ?? 'JWKS_URL');
        }
        if (next === 'mtls') {
            const current = getValue('security.planName');
            if (typeof current !== 'string' || current.trim() === '') updateField('security.planName', 'Default mTLS plan');
        }
    };

    const planName = typeof getValue('security.planName') === 'string' ? (getValue('security.planName') as string) : '';
    const planNameError = errors['security.planName'];

    return (
        <div className="space-y-6">
            <Field orientation="vertical">
                <FieldLabel>
                    Consumer Authentication <span className="text-destructive">*</span>
                </FieldLabel>
                <FieldDescription>Choose how consumers authenticate when calling this API.</FieldDescription>
                <FieldContent>
                    <AuthTypeSelector value={authType} onChange={onChangeAuthType} />
                </FieldContent>
            </Field>

            {authType === 'keyless' ? (
                <div className="rounded-lg border border-success/20 bg-success/5 p-4">
                    <div className="flex items-start gap-3">
                        <SparklesIcon className="mt-0.5 size-4 shrink-0 text-success" aria-hidden="true" />
                        <div className="space-y-1">
                            <p className="text-sm font-medium text-foreground">Keyless (Open)</p>
                            <p className="text-xs leading-relaxed text-muted-foreground">
                                A Keyless plan is open and requires no consumer authentication. No additional configuration is needed.
                            </p>
                        </div>
                    </div>
                </div>
            ) : null}

            {authType === 'api-key' ? (
                <div className="space-y-4 rounded-xl border bg-card/40 p-4 sm:p-5">
                    <p className="text-sm font-semibold">API Key Plan</p>
                    <Field orientation="vertical">
                        <FieldLabel htmlFor="plan-name">
                            Plan Name <span className="text-destructive">*</span>
                        </FieldLabel>
                        <FieldContent>
                            <Input
                                id="plan-name"
                                value={planName}
                                aria-invalid={Boolean(planNameError)}
                                placeholder="e.g. Default API Key plan"
                                onChange={e => updateField('security.planName', e.target.value)}
                            />
                        </FieldContent>
                        <FieldError errors={planNameError ? [{ message: planNameError }] : undefined} />
                    </Field>
                </div>
            ) : null}

            {authType === 'jwt' ? (
                <JwtPlanSection
                    planName={planName}
                    planNameError={planNameError}
                    jwtSignatureOptions={jwtSignatureOptions}
                    jwtResolverOptions={jwtResolverOptions}
                    getValue={getValue}
                    updateField={updateField}
                    errors={errors}
                />
            ) : null}

            {authType === 'oauth2' ? (
                <OAuth2PlanSection
                    planName={planName}
                    planNameError={planNameError}
                    oauth2ResourceOptions={oauth2ResourceOptions}
                    getValue={getValue}
                    updateField={updateField}
                    errors={errors}
                />
            ) : null}

            {authType === 'mtls' ? (
                <div className="space-y-4 rounded-xl border border-border/60 bg-card/40 p-4 sm:p-5">
                    <p className="text-sm font-semibold">Mutual TLS Plan</p>
                    <Field orientation="vertical">
                        <FieldLabel htmlFor="mtls-plan-name">
                            Plan Name <span className="text-destructive">*</span>
                        </FieldLabel>
                        <FieldDescription>
                            Consumers are identified by the X.509 certificate presented during the TLS handshake.
                        </FieldDescription>
                        <FieldContent>
                            <Input
                                id="mtls-plan-name"
                                value={planName}
                                aria-invalid={Boolean(planNameError)}
                                placeholder="e.g. Default mTLS plan"
                                onChange={e => updateField('security.planName', e.target.value)}
                            />
                        </FieldContent>
                        <FieldError errors={planNameError ? [{ message: planNameError }] : undefined} />
                    </Field>
                </div>
            ) : null}
        </div>
    );
}

function JwtPlanSection({
    planName,
    planNameError,
    jwtSignatureOptions,
    jwtResolverOptions,
    getValue,
    updateField,
    errors,
}: Readonly<{
    planName: string;
    planNameError: string | undefined;
    jwtSignatureOptions: SchemaSelectResult;
    jwtResolverOptions: SchemaSelectResult;
    getValue: (path: string) => unknown;
    updateField: (path: string, value: unknown) => void;
    errors: Record<string, string>;
}>) {
    const signature = typeof getValue('security.signature') === 'string' ? (getValue('security.signature') as string) : '';
    const jwksResolver = typeof getValue('security.jwksResolver') === 'string' ? (getValue('security.jwksResolver') as string) : '';
    const resolverParam = typeof getValue('security.resolverParam') === 'string' ? (getValue('security.resolverParam') as string) : '';

    return (
        <div className="space-y-4 rounded-xl border bg-card/40 p-4 sm:p-5">
            <p className="text-sm font-semibold">JWT Plan</p>
            <div className="grid gap-4 md:grid-cols-2">
                <Field orientation="vertical">
                    <FieldLabel htmlFor="jwt-plan-name">
                        JWT Plan Name <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldContent>
                        <Input
                            id="jwt-plan-name"
                            value={planName}
                            aria-invalid={Boolean(planNameError)}
                            placeholder="e.g. Default JWT plan"
                            onChange={e => updateField('security.planName', e.target.value)}
                        />
                    </FieldContent>
                    <FieldError errors={planNameError ? [{ message: planNameError }] : undefined} />
                </Field>

                <Field orientation="vertical">
                    <FieldLabel htmlFor="jwt-signature">
                        Signature <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldContent>
                        <Select
                            value={signature || jwtSignatureOptions?.defaultValue || ''}
                            onValueChange={v => updateField('security.signature', v)}
                        >
                            <SelectTrigger id="jwt-signature" aria-invalid={Boolean(errors['security.signature'])} className="w-full">
                                <SelectValue placeholder="Select…" />
                            </SelectTrigger>
                            <SelectContent>
                                {(jwtSignatureOptions?.options ?? []).map(opt => (
                                    <SelectItem key={opt.value} value={opt.value}>
                                        {opt.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </FieldContent>
                    <FieldError errors={errors['security.signature'] ? [{ message: errors['security.signature'] }] : undefined} />
                </Field>

                <Field orientation="vertical">
                    <FieldLabel htmlFor="jwt-resolver">
                        JWKS Resolver <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldContent>
                        <Select
                            value={jwksResolver || jwtResolverOptions?.defaultValue || ''}
                            onValueChange={v => updateField('security.jwksResolver', v)}
                        >
                            <SelectTrigger id="jwt-resolver" aria-invalid={Boolean(errors['security.jwksResolver'])} className="w-full">
                                <SelectValue placeholder="Select…" />
                            </SelectTrigger>
                            <SelectContent>
                                {(jwtResolverOptions?.options ?? []).map(opt => (
                                    <SelectItem key={opt.value} value={opt.value}>
                                        {opt.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </FieldContent>
                    <FieldError errors={errors['security.jwksResolver'] ? [{ message: errors['security.jwksResolver'] }] : undefined} />
                </Field>

                <Field orientation="vertical">
                    <FieldLabel htmlFor="jwt-resolver-param">
                        Resolver Parameter <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldContent>
                        <Input
                            id="jwt-resolver-param"
                            value={resolverParam}
                            aria-invalid={Boolean(errors['security.resolverParam'])}
                            placeholder="https://idp.example.com/.well-known/jwks.json"
                            onChange={e => updateField('security.resolverParam', e.target.value)}
                        />
                    </FieldContent>
                    <FieldError errors={errors['security.resolverParam'] ? [{ message: errors['security.resolverParam'] }] : undefined} />
                </Field>
            </div>
        </div>
    );
}

function OAuth2PlanSection({
    planName,
    planNameError,
    oauth2ResourceOptions,
    getValue,
    updateField,
    errors,
}: Readonly<{
    planName: string;
    planNameError: string | undefined;
    oauth2ResourceOptions: SchemaSelectResult;
    getValue: (path: string) => unknown;
    updateField: (path: string, value: unknown) => void;
    errors: Record<string, string>;
}>) {
    const resource = typeof getValue('security.resource') === 'string' ? (getValue('security.resource') as string) : '';
    const resourceOptions = oauth2ResourceOptions?.options?.length ? oauth2ResourceOptions.options : OAUTH2_FALLBACK_OPTIONS;
    const resourceError = errors['security.resource'];

    return (
        <div className="space-y-4 rounded-xl border border-border/60 bg-card/40 p-4 sm:p-5">
            <p className="text-sm font-semibold">OAuth 2.0 Plan</p>
            <div className="grid gap-4 md:grid-cols-2">
                <Field orientation="vertical">
                    <FieldLabel htmlFor="oauth2-plan-name">
                        OAuth2 Plan Name <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldDescription>Name shown to consumers when they subscribe.</FieldDescription>
                    <FieldContent>
                        <Input
                            id="oauth2-plan-name"
                            value={planName}
                            aria-invalid={Boolean(planNameError)}
                            placeholder="e.g. Default OAuth2 plan"
                            onChange={e => updateField('security.planName', e.target.value)}
                        />
                    </FieldContent>
                    <FieldError errors={planNameError ? [{ message: planNameError }] : undefined} />
                </Field>

                <Field orientation="vertical">
                    <FieldLabel htmlFor="oauth2-resource">
                        OAuth2 Resource <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldDescription>OAuth2 resource used to validate the token.</FieldDescription>
                    <FieldContent>
                        <Select
                            value={resource || oauth2ResourceOptions?.defaultValue || ''}
                            onValueChange={v => updateField('security.resource', v)}
                        >
                            <SelectTrigger id="oauth2-resource" aria-invalid={Boolean(resourceError)} className="w-full">
                                <SelectValue placeholder="Select an OAuth2 resource" />
                            </SelectTrigger>
                            <SelectContent>
                                {resourceOptions.map(opt => (
                                    <SelectItem key={opt.value} value={opt.value}>
                                        {opt.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </FieldContent>
                    <FieldError errors={resourceError ? [{ message: resourceError }] : undefined} />
                </Field>
            </div>
        </div>
    );
}
