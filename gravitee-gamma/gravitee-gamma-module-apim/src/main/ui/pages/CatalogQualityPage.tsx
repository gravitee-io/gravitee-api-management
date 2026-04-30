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
import { Button } from '@gravitee/graphene-core';
import { AlertCircle, Loader2, RefreshCw, Sparkles } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';

import type { ApiQualityScore } from '../api/catalog-quality-api';
import { fetchCatalogQuality, resolveEnvironmentId } from '../api/catalog-quality-api';
import { AnalysisPanel } from './catalog-quality/AnalysisPanel';
import { ScoreBadge } from './catalog-quality/ScoreBadge';

type SortField = 'name' | 'totalScore' | 'titleScore' | 'descriptionScore';
type SortDir = 'asc' | 'desc';

function useEnvHridFromUrl(): string {
    const parts = window.location.pathname.split('/').filter(Boolean);
    const envIdx = parts.indexOf('environments');
    if (envIdx >= 0 && envIdx + 1 < parts.length) {
        return parts[envIdx + 1]!;
    }
    return 'DEFAULT';
}

export function CatalogQualityPage() {
    const envHrid = useEnvHridFromUrl();
    const [envId, setEnvId] = useState<string | null>(null);
    const [scores, setScores] = useState<ApiQualityScore[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [selectedApi, setSelectedApi] = useState<ApiQualityScore | null>(null);
    const [sortField, setSortField] = useState<SortField>('totalScore');
    const [sortDir, setSortDir] = useState<SortDir>('asc');

    const loadScores = useCallback(
        async (resolvedEnvId: string) => {
            setLoading(true);
            setError(null);
            try {
                const data = await fetchCatalogQuality(resolvedEnvId);
                setScores(data);
            } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to load catalog quality scores');
            } finally {
                setLoading(false);
            }
        },
        [],
    );

    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const resolved = await resolveEnvironmentId(envHrid);
                if (cancelled) return;
                setEnvId(resolved);
                await loadScores(resolved);
            } catch (e) {
                if (!cancelled) {
                    setError(e instanceof Error ? e.message : 'Failed to resolve environment');
                    setLoading(false);
                }
            }
        })();
        return () => {
            cancelled = true;
        };
    }, [envHrid, loadScores]);

    const handleRefresh = useCallback(() => {
        if (envId) loadScores(envId);
    }, [envId, loadScores]);

    const handleSort = useCallback(
        (field: SortField) => {
            if (sortField === field) {
                setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
            } else {
                setSortField(field);
                setSortDir(field === 'name' ? 'asc' : 'asc');
            }
        },
        [sortField],
    );

    const sorted = useMemo(() => {
        return [...scores].sort((a, b) => {
            const mul = sortDir === 'asc' ? 1 : -1;
            if (sortField === 'name') return mul * a.name.localeCompare(b.name);
            return mul * (a[sortField] - b[sortField]);
        });
    }, [scores, sortField, sortDir]);

    const avgScore = useMemo(() => {
        if (scores.length === 0) return 0;
        return Math.round(scores.reduce((sum, s) => sum + s.totalScore, 0) / scores.length);
    }, [scores]);

    if (loading && scores.length === 0) {
        return (
            <div style={{ padding: '40px', textAlign: 'center' }}>
                <Loader2 size={24} style={{ animation: 'spin 1s linear infinite', marginBottom: '12px' }} />
                <p style={{ color: 'var(--muted-foreground, #6b7280)' }}>Loading catalog quality scores...</p>
            </div>
        );
    }

    if (error && scores.length === 0) {
        return (
            <div style={{ padding: '40px', textAlign: 'center' }}>
                <AlertCircle size={24} style={{ color: 'hsl(0 84% 60%)', marginBottom: '12px' }} />
                <p style={{ color: 'hsl(0 84% 40%)' }}>{error}</p>
                <Button type="button" variant="outline" size="sm" onClick={handleRefresh} style={{ marginTop: '12px' }}>
                    Retry
                </Button>
            </div>
        );
    }

    return (
        <div style={{ padding: '24px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
                <div>
                    <h1 style={{ margin: '0 0 4px', fontSize: '22px', fontWeight: 700 }}>Catalog Quality</h1>
                    <p style={{ margin: 0, fontSize: '14px', color: 'var(--muted-foreground, #6b7280)' }}>
                        Analyze and improve API titles and descriptions for better catalog discovery.
                    </p>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '12px', color: 'var(--muted-foreground, #6b7280)' }}>Average Score</div>
                        <ScoreBadge score={avgScore} />
                    </div>
                    <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '12px', color: 'var(--muted-foreground, #6b7280)' }}>APIs</div>
                        <div style={{ fontSize: '18px', fontWeight: 600 }}>{scores.length}</div>
                    </div>
                    <Button type="button" variant="outline" size="sm" onClick={handleRefresh} disabled={loading}>
                        <RefreshCw size={14} style={{ marginRight: '6px' }} />
                        Refresh
                    </Button>
                </div>
            </div>

            {scores.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--muted-foreground, #6b7280)' }}>
                    <Sparkles size={32} style={{ marginBottom: '12px', opacity: 0.5 }} />
                    <p>No published APIs found in this environment.</p>
                </div>
            ) : (
                <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
                        <thead>
                            <tr style={{ borderBottom: '2px solid var(--border, #e5e7eb)' }}>
                                <SortHeader field="name" label="API Name" current={sortField} dir={sortDir} onSort={handleSort} />
                                <th style={{ ...thStyle, width: '30%' }}>Description</th>
                                <SortHeader field="titleScore" label="Title" current={sortField} dir={sortDir} onSort={handleSort} />
                                <SortHeader field="descriptionScore" label="Desc" current={sortField} dir={sortDir} onSort={handleSort} />
                                <SortHeader field="totalScore" label="Score" current={sortField} dir={sortDir} onSort={handleSort} />
                                <th style={{ ...thStyle, width: '100px' }}>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {sorted.map((api) => (
                                <tr
                                    key={api.apiId}
                                    style={{
                                        borderBottom: '1px solid var(--border, #e5e7eb)',
                                        cursor: 'pointer',
                                        backgroundColor: selectedApi?.apiId === api.apiId ? 'var(--accent, #f3f4f6)' : undefined,
                                    }}
                                    onClick={() => setSelectedApi(api)}
                                >
                                    <td style={tdStyle}>
                                        <span style={{ fontWeight: 500 }}>{api.name || '(untitled)'}</span>
                                        {api.definitionVersion && (
                                            <span
                                                style={{
                                                    marginLeft: '6px',
                                                    fontSize: '11px',
                                                    padding: '1px 6px',
                                                    borderRadius: '4px',
                                                    backgroundColor: 'var(--accent, #f3f4f6)',
                                                    color: 'var(--muted-foreground, #6b7280)',
                                                }}
                                            >
                                                {api.definitionVersion}
                                            </span>
                                        )}
                                    </td>
                                    <td style={{ ...tdStyle, color: 'var(--muted-foreground, #6b7280)', maxWidth: '300px' }}>
                                        <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                            {api.description || '(no description)'}
                                        </span>
                                    </td>
                                    <td style={{ ...tdStyle, textAlign: 'center' }}>
                                        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{api.titleScore}/50</span>
                                    </td>
                                    <td style={{ ...tdStyle, textAlign: 'center' }}>
                                        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{api.descriptionScore}/50</span>
                                    </td>
                                    <td style={{ ...tdStyle, textAlign: 'center' }}>
                                        <ScoreBadge score={api.totalScore} />
                                    </td>
                                    <td style={{ ...tdStyle, textAlign: 'center' }}>
                                        <Button
                                            type="button"
                                            variant="ghost"
                                            size="sm"
                                            onClick={(e: React.MouseEvent) => {
                                                e.stopPropagation();
                                                setSelectedApi(api);
                                            }}
                                        >
                                            <Sparkles size={12} style={{ marginRight: '4px' }} />
                                            Analyze
                                        </Button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {selectedApi && envId && (
                <AnalysisPanel
                    api={selectedApi}
                    envId={envId}
                    onClose={() => setSelectedApi(null)}
                    onApplied={handleRefresh}
                />
            )}

            <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
        </div>
    );
}

const thStyle: React.CSSProperties = {
    padding: '10px 12px',
    textAlign: 'left',
    fontSize: '12px',
    fontWeight: 600,
    color: 'var(--muted-foreground, #6b7280)',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
};

const tdStyle: React.CSSProperties = {
    padding: '12px',
};

function SortHeader({
    field,
    label,
    current,
    dir,
    onSort,
}: {
    readonly field: SortField;
    readonly label: string;
    readonly current: SortField;
    readonly dir: SortDir;
    readonly onSort: (f: SortField) => void;
}) {
    const active = current === field;
    return (
        <th style={{ ...thStyle, cursor: 'pointer', userSelect: 'none' }} onClick={() => onSort(field)}>
            {label}
            {active && <span style={{ marginLeft: '4px' }}>{dir === 'asc' ? '\u2191' : '\u2193'}</span>}
        </th>
    );
}
