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
import { Button, Checkbox, Input, Label, Switch } from '@gravitee/graphene-core';
import { ChevronDownIcon, ChevronRightIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import type { ResourceFilteringRule } from '../../../../../types/plan';
import { ALL_HTTP_METHODS } from '../../../../../types/plan';

interface ResourceFilteringFieldsProps {
    rules: ResourceFilteringRule[];
    onChange: (rules: ResourceFilteringRule[]) => void;
    normalizeRequestPath: boolean;
    decodeEncodedSlash: boolean;
    onNormalizeChange: (v: boolean) => void;
    onDecodeSlashChange: (v: boolean) => void;
    readOnly?: boolean;
}

// ─── Collapsible section ──────────────────────────────────────────────────────

function CollapsibleSection({ title, children }: { title: string; children: React.ReactNode }) {
    const [open, setOpen] = useState(true);
    return (
        <div className="rounded-lg border">
            <button type="button" className="flex w-full items-center justify-between px-4 py-3 text-left" onClick={() => setOpen(v => !v)}>
                <span className="text-sm font-medium">{title}</span>
                {open ? (
                    <ChevronDownIcon className="size-4 text-muted-foreground" aria-hidden />
                ) : (
                    <ChevronRightIcon className="size-4 text-muted-foreground" aria-hidden />
                )}
            </button>
            {open && <div className="border-t px-4 py-3 space-y-3">{children}</div>}
        </div>
    );
}

// ─── Rule list inside a section ───────────────────────────────────────────────

function RuleList({
    sectionRules,
    readOnly,
    onAdd,
    onUpdate,
    onRemove,
}: {
    sectionRules: { rule: ResourceFilteringRule; originalIndex: number }[];
    readOnly: boolean;
    onAdd: () => void;
    onUpdate: (originalIndex: number, partial: Partial<ResourceFilteringRule>) => void;
    onRemove: (originalIndex: number) => void;
}) {
    const toggleMethod = (originalIndex: number, method: string, currentMethods: string[]) => {
        const next = currentMethods.includes(method) ? currentMethods.filter(m => m !== method) : [...currentMethods, method];
        onUpdate(originalIndex, { methods: next });
    };

    return (
        <div className="space-y-3">
            {sectionRules.map(({ rule, originalIndex }) => (
                <div key={originalIndex} className="rounded-lg border p-3 space-y-3">
                    <div className="flex items-center justify-between gap-2">
                        <Input
                            value={rule.pattern}
                            onChange={e => onUpdate(originalIndex, { pattern: e.target.value })}
                            placeholder="/api/**"
                            disabled={readOnly}
                            className="flex-1"
                        />
                        {!readOnly && (
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="size-7 shrink-0 text-destructive hover:text-destructive"
                                onClick={() => onRemove(originalIndex)}
                            >
                                <Trash2Icon className="size-3.5" aria-hidden />
                                <span className="sr-only">Remove rule</span>
                            </Button>
                        )}
                    </div>
                    <div className="flex flex-wrap gap-3">
                        {ALL_HTTP_METHODS.map(method => (
                            <div key={method} className="flex items-center gap-1.5">
                                <Checkbox
                                    id={`rf-method-${originalIndex}-${method}`}
                                    checked={rule.methods.includes(method)}
                                    onCheckedChange={() => toggleMethod(originalIndex, method, rule.methods)}
                                    disabled={readOnly}
                                />
                                <label htmlFor={`rf-method-${originalIndex}-${method}`} className="text-xs font-mono cursor-pointer">
                                    {method}
                                </label>
                            </div>
                        ))}
                    </div>
                </div>
            ))}

            {!readOnly && (
                <Button type="button" variant="outline" size="sm" onClick={onAdd}>
                    <PlusIcon className="size-3.5" aria-hidden />
                    Add
                </Button>
            )}
        </div>
    );
}

// ─── Main component ───────────────────────────────────────────────────────────

export function ResourceFilteringFields({
    rules,
    onChange,
    normalizeRequestPath,
    decodeEncodedSlash,
    onNormalizeChange,
    onDecodeSlashChange,
    readOnly = false,
}: Readonly<ResourceFilteringFieldsProps>) {
    const whitelist = rules.map((r, i) => ({ rule: r, originalIndex: i })).filter(x => x.rule.whitelist);
    const blacklist = rules.map((r, i) => ({ rule: r, originalIndex: i })).filter(x => !x.rule.whitelist);

    const addRule = (isWhitelist: boolean) => onChange([...rules, { whitelist: isWhitelist, pattern: '', methods: ['GET'] }]);

    const updateRule = (idx: number, partial: Partial<ResourceFilteringRule>) =>
        onChange(rules.map((r, i) => (i === idx ? { ...r, ...partial } : r)));

    const removeRule = (idx: number) => onChange(rules.filter((_, i) => i !== idx));

    return (
        <div className="space-y-4">
            <CollapsibleSection title="Whitelist">
                <RuleList
                    sectionRules={whitelist}
                    readOnly={readOnly}
                    onAdd={() => addRule(true)}
                    onUpdate={updateRule}
                    onRemove={removeRule}
                />
            </CollapsibleSection>

            <CollapsibleSection title="Blacklist">
                <RuleList
                    sectionRules={blacklist}
                    readOnly={readOnly}
                    onAdd={() => addRule(false)}
                    onUpdate={updateRule}
                    onRemove={removeRule}
                />
            </CollapsibleSection>

            {/* Path options */}
            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                <div className="space-y-0.5">
                    <Label htmlFor="rf-normalize" className="text-sm font-medium">
                        Normalize request path
                    </Label>
                    <p className="text-xs text-muted-foreground">
                        When enabled, the request path is normalized before evaluation (URL-decode, resolve dot segments, collapse double
                        slashes). This prevents bypass via encoded paths.
                    </p>
                </div>
                <Switch id="rf-normalize" checked={normalizeRequestPath} onCheckedChange={onNormalizeChange} disabled={readOnly} />
            </div>

            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                <div className="space-y-0.5">
                    <Label
                        htmlFor="rf-decode-slash"
                        className={`text-sm font-medium${!normalizeRequestPath ? ' text-muted-foreground' : ''}`}
                    >
                        Decode encoded slashes (%2F)
                    </Label>
                    <p className="text-xs text-muted-foreground">
                        Only applies when path normalization is enabled. When enabled, encoded slashes (%2F/%2f) are decoded to
                        &apos;/&apos; during normalization. When disabled (default), they are preserved as literal %2F/%2f so that
                        legitimate uses (e.g. identifiers containing a slash) are not altered. Enable for stricter security at the cost of
                        rejecting paths that legitimately contain encoded slashes.
                    </p>
                </div>
                <Switch
                    id="rf-decode-slash"
                    checked={decodeEncodedSlash}
                    onCheckedChange={onDecodeSlashChange}
                    disabled={readOnly || !normalizeRequestPath}
                />
            </div>
        </div>
    );
}
