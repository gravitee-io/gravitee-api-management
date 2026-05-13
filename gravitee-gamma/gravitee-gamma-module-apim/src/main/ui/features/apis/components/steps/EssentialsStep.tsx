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
import { Input, Label, Textarea } from '@gravitee/graphene-core';
import { ServerIcon, ShieldIcon } from '@gravitee/graphene-core/icons';
import type { CSSProperties } from 'react';

import { SecurityPlanFields } from './SecurityPlanFields';
import { useVerifyContextPath } from '../../hooks/useVerifyContextPath';
import { useApiCreation } from '../../store/apiCreationStore';
import { GATEWAY_URL_PLACEHOLDER } from '../../utils/apiProxyMapper';
import { AUTH_LABEL } from '../../utils/securityFormatters';

export function EssentialsStep() {
    const { state, dispatch } = useApiCreation();
    const { form, validationErrors: errors } = state;
    useVerifyContextPath();

    function update(patch: Partial<typeof form>) {
        dispatch({ type: 'UPDATE_FORM', patch });
    }

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h2 className="text-base font-semibold">Essentials</h2>
                <p className="text-sm text-muted-foreground">
                    Name your API and point it at your backend. Security is pre-configured from the template.
                </p>
            </div>

            <div className="space-y-5">
                {/* Name + Version in same row */}
                <div className="grid gap-4" style={{ gridTemplateColumns: '1fr auto' }}>
                    <div className="space-y-2">
                        <Label htmlFor="essentials-api-name">
                            API Name <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="essentials-api-name"
                            placeholder="e.g. Payment Service API"
                            value={form.apiName}
                            onChange={e => update({ apiName: e.target.value })}
                            aria-invalid={Boolean(errors['apiName'])}
                        />
                        {errors['apiName'] && <p className="text-xs text-destructive">{errors['apiName']}</p>}
                    </div>

                    <div className="space-y-2" style={{ minWidth: '7rem' }}>
                        <Label htmlFor="essentials-api-version">
                            Version <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="essentials-api-version"
                            placeholder="1.0.0"
                            value={form.apiVersion}
                            onChange={e => update({ apiVersion: e.target.value })}
                            aria-invalid={Boolean(errors['apiVersion'])}
                        />
                        {errors['apiVersion'] && <p className="text-xs text-destructive">{errors['apiVersion']}</p>}
                    </div>
                </div>

                {/* Description with char counter */}
                <div className="space-y-2">
                    <Label htmlFor="essentials-description">Description</Label>
                    <Textarea
                        id="essentials-description"
                        placeholder="Describe what this API does and who should use it."
                        value={form.apiDescription}
                        onChange={e => update({ apiDescription: e.target.value })}
                        maxLength={250}
                        rows={3}
                        style={{ fieldSizing: 'fixed' } as unknown as CSSProperties}
                    />
                    <p className="text-xs text-muted-foreground text-right">{form.apiDescription.length}/250</p>
                </div>

                {/* Target URL */}
                <div className="space-y-2">
                    <Label htmlFor="essentials-target-url">
                        Target URL <span className="text-destructive">*</span>
                    </Label>
                    <div className="relative">
                        <ServerIcon
                            className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none"
                            aria-hidden
                        />
                        <Input
                            id="essentials-target-url"
                            placeholder="https://api.internal.example.com"
                            value={form.targetUrl}
                            onChange={e => update({ targetUrl: e.target.value })}
                            aria-invalid={Boolean(errors['targetUrl'])}
                            style={{ paddingLeft: '2.5rem' }}
                        />
                    </div>
                    {errors['targetUrl'] && <p className="text-xs text-destructive">{errors['targetUrl']}</p>}
                    <p className="text-xs text-muted-foreground">The upstream backend the gateway will forward requests to.</p>
                </div>

                {/* Context Path with gateway prefix */}
                <div className="space-y-2">
                    <Label htmlFor="essentials-context-path">
                        Context path <span className="text-destructive">*</span>
                    </Label>
                    <div
                        className="flex items-stretch rounded-md border overflow-hidden"
                        style={errors['contextPath'] ? { borderColor: 'var(--color-destructive)' } : undefined}
                    >
                        <span
                            className="flex items-center px-3 text-sm font-mono text-muted-foreground whitespace-nowrap border-r bg-muted/30 select-none"
                            aria-hidden
                        >
                            {GATEWAY_URL_PLACEHOLDER}
                        </span>
                        <Input
                            id="essentials-context-path"
                            placeholder="/my-api"
                            value={form.contextPath}
                            onChange={e => update({ contextPath: e.target.value })}
                            aria-invalid={Boolean(errors['contextPath'])}
                            style={{ border: 'none', borderRadius: 0, boxShadow: 'none', flex: 1, fontFamily: 'monospace' }}
                        />
                    </div>
                    {errors['contextPath'] && <p className="text-xs text-destructive">{errors['contextPath']}</p>}
                    <p className="text-xs text-muted-foreground">Path prefix clients use to reach this API on the gateway.</p>
                </div>

                {/* Security — pre-configured from template */}
                <div className="space-y-4 rounded-xl border border-primary/20 bg-primary/5 p-4">
                    <div>
                        <div className="flex items-center gap-2">
                            <ShieldIcon className="size-4 shrink-0 text-primary" aria-hidden />
                            <p className="text-sm font-semibold text-primary">Security — {AUTH_LABEL[form.authType]}</p>
                        </div>
                        <p className="mt-1 text-xs text-muted-foreground">
                            Pre-configured from the template. Adjust the plan name below if needed.
                        </p>
                    </div>
                    <SecurityPlanFields showAuthSelector={false} />
                </div>
            </div>
        </div>
    );
}
