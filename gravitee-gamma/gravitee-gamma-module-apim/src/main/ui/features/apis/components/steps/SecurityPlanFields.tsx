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
import { cn, Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';
import { SparklesIcon, CircleCheckIcon } from '@gravitee/graphene-core/icons';

import { useApiCreation } from '../../store/apiCreationStore';
import type { ApiProxyDraft } from '../../types/apiCreation';
import { AUTH_OPTIONS, JWKS_RESOLVERS, JWT_SIGNATURES, OAUTH2_RESOURCES } from '../../utils/securityFormatters';

interface SecurityPlanFieldsProps {
    showAuthSelector?: boolean;
}

export function SecurityPlanFields({ showAuthSelector = true }: SecurityPlanFieldsProps) {
    const { state, dispatch } = useApiCreation();
    const { authType } = state.form;
    const errors = state.validationErrors;

    function update(patch: Partial<ApiProxyDraft>) {
        dispatch({ type: 'UPDATE_FORM', patch });
    }

    return (
        <div className="space-y-5">
            {showAuthSelector && (
                <div className="grid grid-cols-2 gap-3">
                    {AUTH_OPTIONS.map(opt => {
                        const Icon = opt.Icon;
                        const selected = authType === opt.id;
                        return (
                            <button
                                key={opt.id}
                                type="button"
                                onClick={() => update({ authType: opt.id })}
                                className={cn(
                                    'flex items-start gap-3 rounded-xl border-2 p-4 text-left transition-all',
                                    selected ? 'border-primary bg-primary/5' : 'border-border hover:border-foreground/20',
                                )}
                            >
                                <div
                                    className={cn(
                                        'size-10 rounded-lg flex items-center justify-center shrink-0',
                                        selected ? 'bg-primary/10' : 'bg-muted',
                                    )}
                                >
                                    <Icon className="size-5" style={selected ? undefined : opt.iconStyle} aria-hidden />
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2 flex-wrap">
                                        <span className="text-sm font-medium">{opt.label}</span>
                                        {opt.id === 'keyless' && (
                                            <span className="rounded-full px-2 py-0.5 text-xs text-muted-foreground bg-muted">Default</span>
                                        )}
                                    </div>
                                    <p className="text-xs text-muted-foreground mt-1">{opt.description}</p>
                                </div>
                                {selected && <CircleCheckIcon className="size-5 text-primary shrink-0 mt-0.5" aria-hidden />}
                            </button>
                        );
                    })}
                </div>
            )}

            {authType === 'keyless' && (
                <div className="flex items-start gap-3 rounded-lg border border-success/20 bg-success/5 px-4 py-3">
                    <SparklesIcon className="size-4 shrink-0 text-success mt-0.5" aria-hidden />
                    <p className="text-xs text-muted-foreground leading-relaxed">
                        A Keyless plan is open and requires no consumer authentication. No additional configuration is needed.
                    </p>
                </div>
            )}

            {authType === 'api-key' && (
                <div className="rounded-lg border bg-muted/30 p-4 space-y-4">
                    <p className="text-sm font-medium">API Key Plan</p>
                    <div className="space-y-2">
                        <Label htmlFor="apikey-plan-name">
                            API Key Plan Name <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="apikey-plan-name"
                            placeholder="e.g. Default API Key plan"
                            value={state.form.apiKeyPlanName}
                            onChange={e => update({ apiKeyPlanName: e.target.value })}
                            aria-invalid={Boolean(errors['apiKeyPlanName'])}
                        />
                        {errors['apiKeyPlanName'] ? (
                            <p className="text-xs text-destructive">{errors['apiKeyPlanName']}</p>
                        ) : (
                            <p className="text-xs text-muted-foreground">Name shown to consumers when they subscribe to the plan.</p>
                        )}
                    </div>
                </div>
            )}

            {authType === 'jwt' && (
                <div className="rounded-lg border bg-muted/30 p-4 space-y-4">
                    <p className="text-sm font-medium">JWT Plan</p>
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="jwt-plan-name">
                                JWT Plan Name <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="jwt-plan-name"
                                placeholder="e.g. Default JWT plan"
                                value={state.form.jwtPlanName}
                                onChange={e => update({ jwtPlanName: e.target.value })}
                                aria-invalid={Boolean(errors['jwtPlanName'])}
                            />
                            {errors['jwtPlanName'] && <p className="text-xs text-destructive">{errors['jwtPlanName']}</p>}
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="jwt-signature">
                                Signature <span className="text-destructive">*</span>
                            </Label>
                            <Select value={state.form.jwtSignature} onValueChange={v => update({ jwtSignature: v })}>
                                <SelectTrigger id="jwt-signature" className="w-full">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {JWT_SIGNATURES.map(sig => (
                                        <SelectItem key={sig.value} value={sig.value}>
                                            {sig.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="jwt-jwks-resolver">
                                JWKS Resolver <span className="text-destructive">*</span>
                            </Label>
                            <Select value={state.form.jwtJwksResolver} onValueChange={v => update({ jwtJwksResolver: v })}>
                                <SelectTrigger id="jwt-jwks-resolver" className="w-full">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {JWKS_RESOLVERS.map(r => (
                                        <SelectItem key={r.value} value={r.value}>
                                            {r.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="jwt-resolver-parameter">Resolver Parameter</Label>
                            <Input
                                id="jwt-resolver-parameter"
                                placeholder={
                                    state.form.jwtJwksResolver === 'JWKS_URL'
                                        ? 'https://idp.example.com/.well-known/jwks.json'
                                        : state.form.jwtJwksResolver === 'GIVEN_KEY'
                                          ? '-----BEGIN PUBLIC KEY-----...'
                                          : 'Configured globally'
                                }
                                value={state.form.jwtResolverParameter}
                                onChange={e => update({ jwtResolverParameter: e.target.value })}
                            />
                            <p className="text-xs text-muted-foreground">Value used by the selected resolver. Supports EL.</p>
                        </div>
                    </div>
                </div>
            )}

            {authType === 'oauth2' && (
                <div className="rounded-lg border bg-muted/30 p-4 space-y-4">
                    <p className="text-sm font-medium">OAuth 2.0 Plan</p>
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="oauth2-plan-name">
                                OAuth2 Plan Name <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="oauth2-plan-name"
                                placeholder="e.g. Default OAuth2 plan"
                                value={state.form.oauth2PlanName}
                                onChange={e => update({ oauth2PlanName: e.target.value })}
                                aria-invalid={Boolean(errors['oauth2PlanName'])}
                            />
                            {errors['oauth2PlanName'] && <p className="text-xs text-destructive">{errors['oauth2PlanName']}</p>}
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="oauth2-resource">
                                OAuth2 Resource <span className="text-destructive">*</span>
                            </Label>
                            <Select value={state.form.oauth2Resource} onValueChange={v => update({ oauth2Resource: v })}>
                                <SelectTrigger id="oauth2-resource" className="w-full">
                                    <SelectValue placeholder="Select an OAuth2 resource" />
                                </SelectTrigger>
                                <SelectContent>
                                    {OAUTH2_RESOURCES.map(r => (
                                        <SelectItem key={r.value} value={r.value}>
                                            {r.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <p className="text-xs text-muted-foreground">OAuth2 resource used to validate token.</p>
                        </div>
                    </div>
                </div>
            )}

            {authType === 'mtls' && (
                <div className="rounded-lg border bg-muted/30 p-4 space-y-4">
                    <p className="text-sm font-medium">Mutual TLS Plan</p>
                    <div className="space-y-2">
                        <Label htmlFor="mtls-plan-name">
                            mTLS Plan Name <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="mtls-plan-name"
                            placeholder="e.g. Default mTLS plan"
                            value={state.form.mtlsPlanName}
                            onChange={e => update({ mtlsPlanName: e.target.value })}
                            aria-invalid={Boolean(errors['mtlsPlanName'])}
                        />
                        {errors['mtlsPlanName'] ? (
                            <p className="text-xs text-destructive">{errors['mtlsPlanName']}</p>
                        ) : (
                            <p className="text-xs text-muted-foreground">
                                Consumers are identified by the X.509 certificate presented during the TLS handshake.
                            </p>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
