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
import { Card, CardContent, CardHeader, CardTitle, Switch } from '@gravitee/graphene-core';

import { QuotaFields } from './restrictions/QuotaFields';
import { RateLimitFields } from './restrictions/RateLimitFields';
import { ResourceFilteringFields } from './restrictions/ResourceFilteringFields';
import { TokenBudgetFields } from './restrictions/TokenBudgetFields';
import type { PlanContext, RestrictionsFormData } from '../../../../types/plan';
import { EMPTY_RESTRICTIONS } from '../../../../types/plan';

interface PlanRestrictionsStepProps {
    value: RestrictionsFormData;
    onChange: (v: RestrictionsFormData) => void;
    readOnly?: boolean;
    /** Product plans expose Rate Limiting + Token budget; api plans keep the historical set. */
    ctxType?: PlanContext['type'];
}

export function PlanRestrictionsStep({ value, onChange, readOnly = false, ctxType = 'api' }: Readonly<PlanRestrictionsStepProps>) {
    const isProduct = ctxType === 'api-product';

    return (
        <div className="space-y-6">
            {/* Rate Limiting */}
            <Card>
                <CardHeader>
                    <div className="flex items-start justify-between gap-4">
                        <div className="space-y-0.5">
                            <CardTitle className="text-base">Rate Limiting</CardTitle>
                            <p className="text-sm text-muted-foreground">
                                Rate limit how many HTTP requests an application can make in a given period of seconds or minutes.
                            </p>
                        </div>
                        <Switch
                            id="rate-limit-enabled"
                            checked={value.rateLimitEnabled}
                            onCheckedChange={checked =>
                                onChange({
                                    ...value,
                                    rateLimitEnabled: checked,
                                    rateLimit: checked ? value.rateLimit : EMPTY_RESTRICTIONS.rateLimit,
                                })
                            }
                            disabled={readOnly}
                        />
                    </div>
                </CardHeader>
                {value.rateLimitEnabled && (
                    <CardContent>
                        <RateLimitFields
                            value={value.rateLimit}
                            onChange={rateLimit => onChange({ ...value, rateLimit })}
                            readOnly={readOnly}
                        />
                    </CardContent>
                )}
            </Card>

            {/* Token budget — product plans only (enforced by the token-ratelimit policy) */}
            {isProduct && (
                <Card>
                    <CardHeader>
                        <div className="flex items-start justify-between gap-4">
                            <div className="space-y-0.5">
                                <CardTitle className="text-base">Token budget</CardTitle>
                                <p className="text-sm text-muted-foreground">
                                    Limit how many LLM tokens each subscription can consume in a given period.
                                </p>
                            </div>
                            <Switch
                                id="token-budget-enabled"
                                checked={value.tokenBudgetEnabled}
                                onCheckedChange={checked =>
                                    onChange({
                                        ...value,
                                        tokenBudgetEnabled: checked,
                                        tokenBudget: checked ? value.tokenBudget : EMPTY_RESTRICTIONS.tokenBudget,
                                    })
                                }
                                disabled={readOnly}
                            />
                        </div>
                    </CardHeader>
                    {value.tokenBudgetEnabled && (
                        <CardContent>
                            <TokenBudgetFields
                                value={value.tokenBudget}
                                onChange={tokenBudget => onChange({ ...value, tokenBudget })}
                                readOnly={readOnly}
                            />
                        </CardContent>
                    )}
                </Card>
            )}

            {/* Quota */}
            {!isProduct && (
                <Card>
                    <CardHeader>
                        <div className="flex items-start justify-between gap-4">
                            <div className="space-y-0.5">
                                <CardTitle className="text-base">Quota</CardTitle>
                                <p className="text-sm text-muted-foreground">
                                    Rate limit how many HTTP requests an application can make in a given period of hours, days, or months.
                                </p>
                            </div>
                            <Switch
                                id="quota-enabled"
                                checked={value.quotaEnabled}
                                onCheckedChange={checked =>
                                    onChange({
                                        ...value,
                                        quotaEnabled: checked,
                                        quota: checked ? value.quota : EMPTY_RESTRICTIONS.quota,
                                    })
                                }
                                disabled={readOnly}
                            />
                        </div>
                    </CardHeader>
                    {value.quotaEnabled && (
                        <CardContent>
                            <QuotaFields value={value.quota} onChange={quota => onChange({ ...value, quota })} readOnly={readOnly} />
                        </CardContent>
                    )}
                </Card>
            )}

            {/* Resource Filtering */}
            {!isProduct && (
                <Card>
                    <CardHeader>
                        <div className="flex items-start justify-between gap-4">
                            <div className="space-y-0.5">
                                <CardTitle className="text-base">Resource Filtering</CardTitle>
                                <p className="text-sm text-muted-foreground">
                                    Restrict resources according to whitelist and/or blacklist rules.
                                </p>
                            </div>
                            <Switch
                                id="rf-enabled"
                                checked={value.resourceFilteringEnabled}
                                onCheckedChange={checked =>
                                    onChange({
                                        ...value,
                                        resourceFilteringEnabled: checked,
                                        resourceFiltering: checked ? value.resourceFiltering : [],
                                        normalizeRequestPath: checked ? value.normalizeRequestPath : false,
                                        decodeEncodedSlash: checked ? value.decodeEncodedSlash : false,
                                    })
                                }
                                disabled={readOnly}
                            />
                        </div>
                    </CardHeader>
                    {value.resourceFilteringEnabled && (
                        <CardContent>
                            <ResourceFilteringFields
                                rules={value.resourceFiltering}
                                onChange={resourceFiltering => onChange({ ...value, resourceFiltering })}
                                normalizeRequestPath={value.normalizeRequestPath}
                                decodeEncodedSlash={value.decodeEncodedSlash}
                                onNormalizeChange={v =>
                                    onChange({
                                        ...value,
                                        normalizeRequestPath: v,
                                        decodeEncodedSlash: v ? value.decodeEncodedSlash : false,
                                    })
                                }
                                onDecodeSlashChange={v => onChange({ ...value, decodeEncodedSlash: v })}
                                readOnly={readOnly}
                            />
                        </CardContent>
                    )}
                </Card>
            )}
        </div>
    );
}
