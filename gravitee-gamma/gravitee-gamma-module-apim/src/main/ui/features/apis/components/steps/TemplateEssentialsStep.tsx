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
    Textarea,
} from '@gravitee/graphene-core';
import { GlobeIcon, InfoIcon, ShieldIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { usePolicySchema } from '../../hooks/usePolicySchema';
import type { ApiCreationState } from '../../types/models';
import { type SchemaSelectResult, readSchemaSelect } from '../../utils/policySchemaUtils';
import { formatSecurityType } from '../../utils/securityFormatters';

type TemplateEssentialsStepProps = Readonly<{
    data: ApiCreationState;
    errors: Record<string, string>;
    templateLabel: string;
    updateField: (path: string, value: unknown) => void;
}>;

const OAUTH2_FALLBACK_OPTIONS: readonly { value: string; label: string }[] = [
    { value: 'Generic OAuth2 Resource', label: 'Generic OAuth2 Resource' },
    { value: 'Gravitee Access Management', label: 'Gravitee Access Management' },
    { value: 'Keycloak Adapter', label: 'Keycloak Adapter' },
];

export function TemplateEssentialsStep({ data, errors, templateLabel, updateField }: TemplateEssentialsStepProps) {
    const { details, proxy, security } = data;
    const jwtSchema = usePolicySchema('jwt', security.type === 'jwt');
    const oauth2Schema = usePolicySchema('oauth2', security.type === 'oauth2');

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
    const resourceOptions = oauth2ResourceOptions?.options?.length ? oauth2ResourceOptions.options : OAUTH2_FALLBACK_OPTIONS;

    const planName = 'planName' in security ? security.planName : undefined;
    const oauth2Resource = security.type === 'oauth2' ? security.resource : '';

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="space-y-1">
                <div className="flex items-center gap-2">
                    <InfoIcon className="size-5 shrink-0 text-primary" aria-hidden="true" />
                    <h2 className="text-base font-semibold">Essentials</h2>
                </div>
                <p className="text-sm text-muted-foreground">
                    Required fields are marked with <span className="text-destructive">*</span>.
                </p>
            </div>

            {/* API Identity */}
            <section className="space-y-4">
                <h3 className="text-sm font-semibold text-foreground">API Identity</h3>

                {/* Name + Version side by side — matches prototype layout */}
                <div className="grid grid-cols-2 gap-4">
                    <Field orientation="vertical">
                        <FieldLabel htmlFor="ess-api-name">
                            API Name <span className="text-destructive">*</span>
                        </FieldLabel>
                        <FieldDescription>Public name shown to consumers in the Developer Portal.</FieldDescription>
                        <FieldContent>
                            <Input
                                id="ess-api-name"
                                value={details.name}
                                aria-invalid={Boolean(errors['details.name'])}
                                placeholder="e.g. Payment Gateway API"
                                onChange={e => updateField('details.name', e.target.value)}
                            />
                        </FieldContent>
                        <FieldError errors={errors['details.name'] ? [{ message: errors['details.name'] }] : undefined} />
                    </Field>

                    <Field orientation="vertical">
                        <FieldLabel htmlFor="ess-api-version">
                            Version <span className="text-destructive">*</span>
                        </FieldLabel>
                        <FieldDescription>For example 1.0.0, 2.1.</FieldDescription>
                        <FieldContent>
                            <Input
                                id="ess-api-version"
                                value={details.version}
                                aria-invalid={Boolean(errors['details.version'])}
                                placeholder="e.g. 1.0.0"
                                onChange={e => updateField('details.version', e.target.value)}
                            />
                        </FieldContent>
                        <FieldError errors={errors['details.version'] ? [{ message: errors['details.version'] }] : undefined} />
                    </Field>
                </div>

                <Field orientation="vertical">
                    <FieldLabel htmlFor="ess-api-description">Description</FieldLabel>
                    <FieldDescription>Optional. Max 250 characters.</FieldDescription>
                    <FieldContent>
                        <Textarea
                            id="ess-api-description"
                            value={details.description}
                            aria-invalid={Boolean(errors['details.description'])}
                            placeholder="Describe what this API does…"
                            rows={3}
                            onChange={e => updateField('details.description', e.target.value)}
                        />
                    </FieldContent>
                    <FieldError errors={errors['details.description'] ? [{ message: errors['details.description'] }] : undefined} />
                </Field>
            </section>

            {/* Proxy Configuration */}
            <section className="space-y-4">
                <h3 className="text-sm font-semibold text-foreground">Proxy Configuration</h3>

                <Field orientation="vertical">
                    <FieldLabel htmlFor="ess-context-path">
                        Context Path <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldDescription>Gateway path consumers use to reach this API. Must start with /.</FieldDescription>
                    <FieldContent>
                        <Input
                            id="ess-context-path"
                            value={proxy.contextPath}
                            aria-invalid={Boolean(errors['proxy.contextPath'])}
                            placeholder="e.g. /my-api"
                            onChange={e => updateField('proxy.contextPath', e.target.value)}
                        />
                    </FieldContent>
                    <FieldError errors={errors['proxy.contextPath'] ? [{ message: errors['proxy.contextPath'] }] : undefined} />
                </Field>

                <Field orientation="vertical">
                    <FieldLabel htmlFor="ess-target-url">
                        Upstream URL <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldDescription>Where the gateway forwards traffic to.</FieldDescription>
                    <FieldContent>
                        <div className="relative">
                            <GlobeIcon
                                className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none"
                                aria-hidden="true"
                            />
                            <Input
                                id="ess-target-url"
                                type="url"
                                value={proxy.targetUrl}
                                aria-invalid={Boolean(errors['proxy.targetUrl'])}
                                placeholder="https://upstream.example.com"
                                className="font-mono"
                                style={{ paddingLeft: '2.5rem' }}
                                onChange={e => updateField('proxy.targetUrl', e.target.value)}
                            />
                        </div>
                    </FieldContent>
                    <FieldError errors={errors['proxy.targetUrl'] ? [{ message: errors['proxy.targetUrl'] }] : undefined} />
                </Field>
            </section>

            {/* API Key — plan name */}
            {security.type === 'api-key' ? (
                <div className="space-y-4 rounded-xl border border-primary/20 bg-primary/5 p-4 sm:p-5">
                    <div>
                        <div className="flex items-center gap-2">
                            <ShieldIcon className="size-4 shrink-0 text-primary" aria-hidden="true" />
                            <p className="text-sm font-semibold text-primary">Security Configuration</p>
                        </div>
                        <p className="mt-1 text-xs text-muted-foreground">
                            Complete the required fields for the {formatSecurityType(security.type)} plan created by this template.
                        </p>
                    </div>
                    <Field orientation="vertical">
                        <FieldLabel htmlFor="ess-apikey-plan-name">
                            Plan Name <span className="text-destructive">*</span>
                        </FieldLabel>
                        <FieldContent>
                            <Input
                                id="ess-apikey-plan-name"
                                value={security.planName}
                                aria-invalid={Boolean(errors['security.planName'])}
                                placeholder="e.g. Default API Key plan"
                                onChange={e => updateField('security.planName', e.target.value)}
                            />
                        </FieldContent>
                        <FieldError errors={errors['security.planName'] ? [{ message: errors['security.planName'] }] : undefined} />
                    </Field>
                </div>
            ) : null}

            {/* JWT — full plan configuration */}
            {security.type === 'jwt' ? (
                <div className="space-y-4 rounded-xl border border-primary/20 bg-primary/5 p-4 sm:p-5">
                    <div>
                        <div className="flex items-center gap-2">
                            <ShieldIcon className="size-4 shrink-0 text-primary" aria-hidden="true" />
                            <p className="text-sm font-semibold text-primary">Security Configuration</p>
                        </div>
                        <p className="mt-1 text-xs text-muted-foreground">
                            Complete the required fields for the {formatSecurityType(security.type)} plan created by this template.
                        </p>
                    </div>
                    <div className="grid gap-4 md:grid-cols-2">
                        <Field orientation="vertical">
                            <FieldLabel htmlFor="ess-jwt-plan-name">
                                JWT Plan Name <span className="text-destructive">*</span>
                            </FieldLabel>
                            <FieldContent>
                                <Input
                                    id="ess-jwt-plan-name"
                                    value={security.planName}
                                    aria-invalid={Boolean(errors['security.planName'])}
                                    placeholder="e.g. Default JWT plan"
                                    onChange={e => updateField('security.planName', e.target.value)}
                                />
                            </FieldContent>
                            <FieldError errors={errors['security.planName'] ? [{ message: errors['security.planName'] }] : undefined} />
                        </Field>

                        <Field orientation="vertical">
                            <FieldLabel htmlFor="ess-jwt-signature">
                                Signature <span className="text-destructive">*</span>
                            </FieldLabel>
                            <FieldContent>
                                <Select
                                    value={security.signature || jwtSignatureOptions?.defaultValue || ''}
                                    onValueChange={v => updateField('security.signature', v)}
                                >
                                    <SelectTrigger
                                        id="ess-jwt-signature"
                                        aria-invalid={Boolean(errors['security.signature'])}
                                        className="w-full"
                                    >
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
                            <FieldLabel htmlFor="ess-jwt-resolver">
                                JWKS Resolver <span className="text-destructive">*</span>
                            </FieldLabel>
                            <FieldContent>
                                <Select
                                    value={security.jwksResolver || jwtResolverOptions?.defaultValue || ''}
                                    onValueChange={v => updateField('security.jwksResolver', v)}
                                >
                                    <SelectTrigger
                                        id="ess-jwt-resolver"
                                        aria-invalid={Boolean(errors['security.jwksResolver'])}
                                        className="w-full"
                                    >
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
                            <FieldError
                                errors={errors['security.jwksResolver'] ? [{ message: errors['security.jwksResolver'] }] : undefined}
                            />
                        </Field>

                        <Field orientation="vertical">
                            <FieldLabel htmlFor="ess-jwt-resolver-param">
                                Resolver Parameter <span className="text-destructive">*</span>
                            </FieldLabel>
                            <FieldContent>
                                <Input
                                    id="ess-jwt-resolver-param"
                                    value={security.resolverParam}
                                    aria-invalid={Boolean(errors['security.resolverParam'])}
                                    placeholder="https://idp.example.com/.well-known/jwks.json"
                                    onChange={e => updateField('security.resolverParam', e.target.value)}
                                />
                            </FieldContent>
                            <FieldError
                                errors={errors['security.resolverParam'] ? [{ message: errors['security.resolverParam'] }] : undefined}
                            />
                        </Field>
                    </div>
                </div>
            ) : null}

            {/* OAuth2 — resource select; token introspection pre-configured by template */}
            {security.type === 'oauth2' ? (
                <div className="space-y-4 rounded-xl border border-primary/20 bg-primary/5 p-4 sm:p-5">
                    <div>
                        <div className="flex items-center gap-2">
                            <ShieldIcon className="size-4 shrink-0 text-primary" aria-hidden="true" />
                            <p className="text-sm font-semibold text-primary">Security Configuration</p>
                        </div>
                        <p className="mt-1 text-xs text-muted-foreground">
                            Complete the required fields for the {formatSecurityType(security.type)} plan created by this template.
                        </p>
                    </div>
                    <Field orientation="vertical">
                        <FieldLabel htmlFor="ess-oauth2-resource">
                            OAuth2 Resource <span className="text-destructive">*</span>
                        </FieldLabel>
                        <FieldDescription>OAuth2 resource used to validate the access token.</FieldDescription>
                        <FieldContent>
                            <Select
                                value={oauth2Resource || oauth2ResourceOptions?.defaultValue || ''}
                                onValueChange={v => updateField('security.resource', v)}
                            >
                                <SelectTrigger
                                    id="ess-oauth2-resource"
                                    aria-invalid={Boolean(errors['security.resource'])}
                                    className="w-full"
                                >
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
                        <FieldError errors={errors['security.resource'] ? [{ message: errors['security.resource'] }] : undefined} />
                    </Field>
                </div>
            ) : null}

            {/* Pre-configured by template info box */}
            <div className="rounded-lg border border-primary/20 bg-primary/5 p-3">
                <div className="flex flex-wrap items-center gap-2">
                    <ShieldIcon className="size-4 shrink-0 text-primary" aria-hidden="true" />
                    <span className="text-xs font-medium text-primary">Pre-configured by {templateLabel}:</span>
                    <Badge variant="secondary" className="border border-primary/20 bg-primary/10 text-primary text-xs">
                        {formatSecurityType(security.type)}
                    </Badge>
                    {planName ? (
                        <Badge variant="secondary" className="border border-primary/20 bg-primary/10 text-primary text-xs">
                            {planName}
                        </Badge>
                    ) : null}
                </div>
            </div>
        </div>
    );
}
