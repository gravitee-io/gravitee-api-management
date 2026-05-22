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
import { Button, cn } from '@gravitee/graphene-core';
import { Check, Code2, Copy, List } from 'lucide-react';
import { useMemo, useState } from 'react';
import { statementsToGapl, type PolicyStatement } from './statement-to-gapl';

interface GaplPreviewProps {
    readonly policyName: string;
    readonly target?: { label: string } | null;
    readonly statements: readonly PolicyStatement[];
}

export function GaplPreview({ policyName, target, statements }: GaplPreviewProps) {
    const [view, setView] = useState<'form' | 'code'>('code');
    const [copied, setCopied] = useState(false);

    const code = useMemo(
        () => statementsToGapl(policyName || 'new-policy', statements, target ?? undefined),
        [policyName, statements, target],
    );

    const totalPrincipals = new Set(statements.flatMap(s => s.principals.map(p => p.id))).size;
    const totalActions = new Set(statements.flatMap(s => s.actions.map(a => a.id))).size;
    const totalResources = new Set(statements.flatMap(s => s.resources.map(r => r.id))).size;

    const handleCopy = async () => {
        await navigator.clipboard.writeText(code);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <div className="flex h-full flex-col overflow-hidden rounded-xl border bg-card">
            <div className="flex items-center justify-between gap-2 border-b px-4 py-3">
                <div className="min-w-0">
                    <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Preview</p>
                    <p className="truncate text-sm font-medium">Gravitee Authorization Policy Language (GAPL)</p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                    <div className="inline-flex rounded-md border bg-muted p-0.5 text-xs">
                        <button
                            type="button"
                            className={cn(
                                'flex items-center gap-1 rounded px-2 py-1',
                                view === 'form' ? 'bg-background shadow-sm' : 'text-muted-foreground',
                            )}
                            onClick={() => setView('form')}
                        >
                            <List className="size-3" />
                            Form
                        </button>
                        <button
                            type="button"
                            className={cn(
                                'flex items-center gap-1 rounded px-2 py-1',
                                view === 'code' ? 'bg-background shadow-sm' : 'text-muted-foreground',
                            )}
                            onClick={() => setView('code')}
                        >
                            <Code2 className="size-3" />
                            Code
                        </button>
                    </div>
                    <Button variant="ghost" size="sm" onClick={handleCopy} className="h-7 gap-1 px-2" title="Copy GAPL to clipboard">
                        {copied ? <Check className="size-3" /> : <Copy className="size-3" />}
                        {copied ? 'Copied' : 'Copy'}
                    </Button>
                </div>
            </div>

            <div className="flex-1 overflow-auto">
                {view === 'code' ? (
                    <pre
                        className="whitespace-pre-wrap break-words px-4 py-3 font-mono leading-relaxed text-foreground"
                        style={{ fontSize: '11px' }}
                    >
                        {code}
                    </pre>
                ) : (
                    <div className="divide-y">
                        {statements.map((stmt, idx) => (
                            <div key={stmt.id} className="px-4 py-3 text-xs">
                                <div className="mb-2 flex items-center gap-2">
                                    <span
                                        className={cn(
                                            'rounded px-1.5 py-0.5 uppercase tracking-wide',
                                            stmt.effect === 'permit'
                                                ? 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-400'
                                                : 'bg-red-500/15 text-red-700 dark:text-red-400',
                                        )}
                                        style={{ fontSize: '10px' }}
                                    >
                                        {stmt.effect}
                                    </span>
                                    <span className="text-muted-foreground">Statement {idx + 1}</span>
                                </div>
                                <dl className="grid gap-y-1" style={{ gridTemplateColumns: '88px 1fr' }}>
                                    <dt className="text-muted-foreground">Principals</dt>
                                    <dd>{stmt.principals.map(p => p.label).join(', ') || '—'}</dd>
                                    <dt className="text-muted-foreground">Actions</dt>
                                    <dd>{stmt.actions.map(a => a.label).join(', ') || '—'}</dd>
                                    <dt className="text-muted-foreground">Resources</dt>
                                    <dd>{stmt.resources.map(r => r.label).join(', ') || '—'}</dd>
                                    {stmt.condition && stmt.condition.trim() ? (
                                        <>
                                            <dt className="text-muted-foreground">Condition</dt>
                                            <dd className="font-mono" style={{ fontSize: '11px' }}>
                                                {stmt.condition}
                                            </dd>
                                        </>
                                    ) : null}
                                </dl>
                            </div>
                        ))}
                        {statements.length === 0 && (
                            <div className="px-4 py-6 text-center text-sm text-muted-foreground">
                                No statements yet. Add a statement in the builder.
                            </div>
                        )}
                    </div>
                )}
            </div>

            <div className="border-t px-4 py-3 text-xs text-muted-foreground">
                <div className="grid grid-cols-3 gap-2">
                    <GaplStat label="Principals" value={totalPrincipals} />
                    <GaplStat label="Actions" value={totalActions} />
                    <GaplStat label="Resources" value={totalResources} />
                </div>
            </div>
        </div>
    );
}

function GaplStat({ label, value }: { label: string; value: number }) {
    return (
        <div className="rounded border bg-background px-2 py-1.5">
            <p className="text-sm font-medium text-foreground">{value}</p>
            <p className="uppercase tracking-wide" style={{ fontSize: '10px' }}>
                {label}
            </p>
        </div>
    );
}
