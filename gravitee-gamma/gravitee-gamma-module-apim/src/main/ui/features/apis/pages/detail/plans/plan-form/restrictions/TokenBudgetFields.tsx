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
import { Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

import type { TokenBudgetFormData } from '../../../../../types/plan';

interface TokenBudgetFieldsProps {
    value: TokenBudgetFormData;
    onChange: (v: TokenBudgetFormData) => void;
    readOnly?: boolean;
}

export function TokenBudgetFields({ value, onChange, readOnly = false }: Readonly<TokenBudgetFieldsProps>) {
    return (
        <div className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="token-budget-limit">Default token limit per developer</Label>
                <Input
                    id="token-budget-limit"
                    type="number"
                    min={1}
                    value={value.limit}
                    onChange={e => onChange({ ...value, limit: Math.max(1, Number(e.target.value)) })}
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    Each developer&apos;s personal limit is set when you add them on the Developers tab — this is only the fallback for
                    developers without one. A token = one unit of LLM input/output; exceeding the limit returns HTTP 429 until the period
                    resets.
                </p>
            </div>

            <div className="flex gap-4">
                <div className="space-y-2 flex-1">
                    <Label htmlFor="token-budget-period">Reset period</Label>
                    <Input
                        id="token-budget-period"
                        type="number"
                        min={1}
                        value={value.period}
                        onChange={e => onChange({ ...value, period: Math.max(1, Number(e.target.value)) })}
                        disabled={readOnly}
                    />
                </div>
                <div className="space-y-2 flex-1">
                    <Label htmlFor="token-budget-unit">Period unit</Label>
                    <Select
                        value={value.unit}
                        onValueChange={v => onChange({ ...value, unit: v as TokenBudgetFormData['unit'] })}
                        disabled={readOnly}
                    >
                        <SelectTrigger id="token-budget-unit" className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="MINUTES">MINUTES</SelectItem>
                            <SelectItem value="HOURS">HOURS</SelectItem>
                            <SelectItem value="DAYS">DAYS</SelectItem>
                        </SelectContent>
                    </Select>
                </div>
            </div>
            <p className="text-xs text-muted-foreground">
                Consumers exceeding the budget receive HTTP 429 until the period resets. Responses include X-Token-Rate-Limit-Remaining so
                consumers can track usage.
            </p>
        </div>
    );
}
