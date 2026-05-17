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
    Button,
    Checkbox,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Separator,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import type { ResourceFilteringRule } from '../../../../features/apis/types/plan';
import { ALL_HTTP_METHODS } from '../../../../features/apis/types/plan';

interface ResourceFilteringFieldsProps {
    rules: ResourceFilteringRule[];
    onChange: (rules: ResourceFilteringRule[]) => void;
    readOnly?: boolean;
}

const EMPTY_RULE: ResourceFilteringRule = { whitelist: true, pattern: '', methods: ['GET'] };

export function ResourceFilteringFields({ rules, onChange, readOnly = false }: Readonly<ResourceFilteringFieldsProps>) {
    const addRule = () => onChange([...rules, { ...EMPTY_RULE }]);

    const updateRule = (idx: number, partial: Partial<ResourceFilteringRule>) => {
        const next = rules.map((r, i) => (i === idx ? { ...r, ...partial } : r));
        onChange(next);
    };

    const removeRule = (idx: number) => onChange(rules.filter((_, i) => i !== idx));

    const toggleMethod = (idx: number, method: string) => {
        const current = rules[idx].methods;
        const next = current.includes(method) ? current.filter(m => m !== method) : [...current, method];
        updateRule(idx, { methods: next });
    };

    return (
        <div className="space-y-4">
            {rules.length === 0 && (
                <p className="text-sm text-muted-foreground">No rules defined. Add a rule to restrict resource access.</p>
            )}

            {rules.map((rule, idx) => (
                <div key={idx} className="rounded-lg border p-4 space-y-3">
                    <div className="flex items-center justify-between">
                        <p className="text-sm font-medium">Rule {idx + 1}</p>
                        {!readOnly && (
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="size-7 text-destructive hover:text-destructive"
                                onClick={() => removeRule(idx)}
                            >
                                <Trash2Icon className="size-3.5" aria-hidden />
                                <span className="sr-only">Remove rule</span>
                            </Button>
                        )}
                    </div>

                    <div className="grid gap-3" style={{ gridTemplateColumns: '140px 1fr' }}>
                        <div className="space-y-1.5">
                            <Label htmlFor={`rf-type-${idx}`}>Type</Label>
                            <Select
                                value={rule.whitelist ? 'allow' : 'deny'}
                                onValueChange={v => updateRule(idx, { whitelist: v === 'allow' })}
                                disabled={readOnly}
                            >
                                <SelectTrigger id={`rf-type-${idx}`}>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="allow">Allow</SelectItem>
                                    <SelectItem value="deny">Deny</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="space-y-1.5">
                            <Label htmlFor={`rf-pattern-${idx}`}>Path pattern</Label>
                            <Input
                                id={`rf-pattern-${idx}`}
                                value={rule.pattern}
                                onChange={e => updateRule(idx, { pattern: e.target.value })}
                                placeholder="/api/**"
                                disabled={readOnly}
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <Label className="text-xs text-muted-foreground">HTTP Methods</Label>
                        <div className="flex flex-wrap gap-3">
                            {ALL_HTTP_METHODS.map(method => (
                                <div key={method} className="flex items-center gap-1.5">
                                    <Checkbox
                                        id={`rf-method-${idx}-${method}`}
                                        checked={rule.methods.includes(method)}
                                        onCheckedChange={() => toggleMethod(idx, method)}
                                        disabled={readOnly}
                                    />
                                    <label htmlFor={`rf-method-${idx}-${method}`} className="text-xs font-mono cursor-pointer">
                                        {method}
                                    </label>
                                </div>
                            ))}
                        </div>
                    </div>

                    {idx < rules.length - 1 && <Separator />}
                </div>
            ))}

            {!readOnly && (
                <Button type="button" variant="outline" size="sm" onClick={addRule}>
                    <PlusIcon className="size-3.5" aria-hidden />
                    Add rule
                </Button>
            )}
        </div>
    );
}
