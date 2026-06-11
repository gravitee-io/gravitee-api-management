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
import { Alert, AlertDescription, Card, CardContent, CardHeader, CardTitle, Input, Label, Separator } from '@gravitee/graphene-core';
import { ChevronDownIcon, ChevronRightIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { ApiKeySecurityFields, DEFAULT_API_KEY_CONFIG } from './security/ApiKeySecurityFields';
import type { ApiKeyConfig } from './security/ApiKeySecurityFields';
import { DEFAULT_JWT_CONFIG, JwtSecurityFields } from './security/JwtSecurityFields';
import type { JwtConfig } from './security/JwtSecurityFields';
import { MtlsSecurityFields } from './security/MtlsSecurityFields';
import { DEFAULT_OAUTH2_CONFIG, OAuth2SecurityFields } from './security/OAuth2SecurityFields';
import type { OAuth2Config } from './security/OAuth2SecurityFields';
import type { ApiResourceOption } from './security/ResourceSelectInput';
import { useApiDetail } from '../../../../hooks/useApiDetail';
import { useResourcePlugins } from '../../../../hooks/useApiResources';
import type { PlanContext, PlanSecurityType, SecurityFormData } from '../../../../types/plan';
import { PLAN_SECURITY_LABELS } from '../../../../types/plan';

// Deep-merge stored JWT config with defaults, handling nested objects correctly.
function resolveJwtConfig(stored: Partial<JwtConfig>): JwtConfig {
    return {
        ...DEFAULT_JWT_CONFIG,
        ...stored,
        confirmationMethodValidation: {
            ...DEFAULT_JWT_CONFIG.confirmationMethodValidation,
            ...(stored.confirmationMethodValidation ?? {}),
            certificateBoundThumbprint: {
                ...DEFAULT_JWT_CONFIG.confirmationMethodValidation.certificateBoundThumbprint,
                ...(stored.confirmationMethodValidation?.certificateBoundThumbprint ?? {}),
            },
        },
        tokenTypValidation: {
            ...DEFAULT_JWT_CONFIG.tokenTypValidation,
            ...(stored.tokenTypValidation ?? {}),
        },
        revocationCheck: {
            ...DEFAULT_JWT_CONFIG.revocationCheck,
            ...(stored.revocationCheck ?? {}),
            auth: {
                ...DEFAULT_JWT_CONFIG.revocationCheck.auth,
                ...(stored.revocationCheck?.auth ?? {}),
            },
        },
    };
}

interface PlanSecurityStepProps {
    ctx: PlanContext;
    securityType: PlanSecurityType;
    value: SecurityFormData;
    onChange: (v: SecurityFormData) => void;
    readOnly?: boolean;
}

export function PlanSecurityStep({ ctx, securityType, value, onChange, readOnly = false }: Readonly<PlanSecurityStepProps>) {
    const [selectionRuleOpen, setSelectionRuleOpen] = useState(false);
    const updateConfig = (cfg: unknown) => onChange({ ...value, configuration: cfg as Record<string, unknown> });

    // Resources live on the API definition; suggest them (with plugin icons) in the OAuth2 resource pickers.
    const apiId = ctx.type === 'api' ? ctx.entityId : undefined;
    const { data: api } = useApiDetail(apiId);
    const { data: plugins = [] } = useResourcePlugins();
    const resourceOptions = useMemo<ApiResourceOption[]>(() => {
        const iconByType = new Map(plugins.map(p => [p.id, p.icon]));
        return (api?.resources ?? []).map(r => ({ name: r.name, type: r.type, icon: iconByType.get(r.type) }));
    }, [api?.resources, plugins]);

    return (
        <div className="space-y-6">
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">{PLAN_SECURITY_LABELS[securityType]} authentication configuration</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    {securityType === 'API_KEY' && (
                        <ApiKeySecurityFields
                            value={{ ...DEFAULT_API_KEY_CONFIG, ...(value.configuration as Partial<ApiKeyConfig>) }}
                            onChange={updateConfig}
                            readOnly={readOnly}
                        />
                    )}
                    {securityType === 'JWT' && (
                        <JwtSecurityFields
                            value={resolveJwtConfig(value.configuration as Partial<JwtConfig>)}
                            onChange={updateConfig}
                            readOnly={readOnly}
                        />
                    )}
                    {securityType === 'OAUTH2' && (
                        <OAuth2SecurityFields
                            value={{ ...DEFAULT_OAUTH2_CONFIG, ...(value.configuration as Partial<OAuth2Config>) }}
                            onChange={updateConfig}
                            readOnly={readOnly}
                            resourceOptions={resourceOptions}
                        />
                    )}
                    {securityType === 'MTLS' && <MtlsSecurityFields />}
                </CardContent>
            </Card>

            {/* Additional selection rule — shown for all types except KEY_LESS (this step isn't rendered for KEY_LESS) */}
            <Card>
                <CardHeader className="cursor-pointer select-none" onClick={() => setSelectionRuleOpen(v => !v)}>
                    <div className="flex items-center justify-between">
                        <CardTitle className="text-base">Additional selection rule</CardTitle>
                        {selectionRuleOpen ? (
                            <ChevronDownIcon className="size-4 text-muted-foreground" aria-hidden />
                        ) : (
                            <ChevronRightIcon className="size-4 text-muted-foreground" aria-hidden />
                        )}
                    </div>
                </CardHeader>
                {selectionRuleOpen && (
                    <CardContent className="space-y-3">
                        <Alert>
                            <AlertDescription>
                                Define an additional rule when managing multiple plans of the same type to improve the plan&apos;s selection
                                process.
                            </AlertDescription>
                        </Alert>
                        <p className="text-xs text-muted-foreground">
                            Example:{' '}
                            <code className="font-mono bg-muted px-1 rounded text-xs">
                                {`{#context.attributes['jwt'].claims['iss'] == 'my-issuer'}`}
                            </code>
                        </p>
                        <Separator />
                        <div className="space-y-2">
                            <Label htmlFor="selection-rule">Selection Rule</Label>
                            <Input
                                id="selection-rule"
                                value={value.selectionRule}
                                onChange={e => onChange({ ...value, selectionRule: e.target.value })}
                                placeholder="EL expression (optional)"
                                disabled={readOnly}
                            />
                            <p className="text-xs text-muted-foreground">Supports Expression Language (EL).</p>
                        </div>
                    </CardContent>
                )}
            </Card>
        </div>
    );
}
