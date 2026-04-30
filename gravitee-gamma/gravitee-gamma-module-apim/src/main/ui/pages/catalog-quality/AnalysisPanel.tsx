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
import { AlertCircle, ArrowRight, Check, Loader2, Sparkles, X } from 'lucide-react';
import { useCallback, useState } from 'react';

import type { ApiQualityScore, ApiSuggestion } from '../../api/catalog-quality-api';
import { applyApiSuggestion, fetchApiSuggestions } from '../../api/catalog-quality-api';
import { ScoreBadge } from './ScoreBadge';

interface AnalysisPanelProps {
    readonly api: ApiQualityScore;
    readonly envId: string;
    readonly onClose: () => void;
    readonly onApplied: () => void;
}

export function AnalysisPanel({ api, envId, onClose, onApplied }: AnalysisPanelProps) {
    const [suggestion, setSuggestion] = useState<ApiSuggestion | null>(null);
    const [loading, setLoading] = useState(false);
    const [applying, setApplying] = useState(false);
    const [applied, setApplied] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const canApply = api.definitionVersion === 'V4' && api.apiType === 'PROXY';

    const handleAnalyze = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const result = await fetchApiSuggestions(api.apiId, envId);
            setSuggestion(result);
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to generate suggestions');
        } finally {
            setLoading(false);
        }
    }, [api.apiId, envId]);

    const handleApply = useCallback(async () => {
        if (!suggestion) return;
        setApplying(true);
        setError(null);
        try {
            await applyApiSuggestion(api.apiId, envId, suggestion.suggestedTitle, suggestion.suggestedDescription);
            setApplied(true);
            onApplied();
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to apply suggestion');
        } finally {
            setApplying(false);
        }
    }, [api.apiId, envId, suggestion, onApplied]);

    return (
        <div
            style={{
                position: 'fixed',
                top: 0,
                right: 0,
                width: '520px',
                height: '100vh',
                backgroundColor: 'var(--background, #fff)',
                borderLeft: '1px solid var(--border, #e5e7eb)',
                boxShadow: '-4px 0 24px rgba(0,0,0,0.08)',
                display: 'flex',
                flexDirection: 'column',
                zIndex: 50,
                overflow: 'hidden',
            }}
        >
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '16px 20px',
                    borderBottom: '1px solid var(--border, #e5e7eb)',
                }}
            >
                <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>Quality Analysis</h3>
                <button
                    onClick={onClose}
                    style={{
                        border: 'none',
                        background: 'none',
                        cursor: 'pointer',
                        padding: '4px',
                        borderRadius: '4px',
                        display: 'flex',
                    }}
                >
                    <X size={18} />
                </button>
            </div>

            <div style={{ flex: 1, overflow: 'auto', padding: '20px' }}>
                <div style={{ marginBottom: '20px' }}>
                    <h4 style={{ margin: '0 0 4px', fontSize: '15px', fontWeight: 600 }}>{api.name || '(no title)'}</h4>
                    <p style={{ margin: 0, fontSize: '13px', color: 'var(--muted-foreground, #6b7280)' }}>
                        {api.description ? api.description.substring(0, 150) + (api.description.length > 150 ? '...' : '') : '(no description)'}
                    </p>
                </div>

                <div style={{ display: 'flex', gap: '12px', marginBottom: '24px' }}>
                    <ScoreCard label="Overall" score={api.totalScore} max={100} />
                    <ScoreCard label="Title" score={api.titleScore} max={50} />
                    <ScoreCard label="Description" score={api.descriptionScore} max={50} />
                </div>

                {api.titleIssues.length > 0 && <IssueList label="Title Issues" issues={api.titleIssues} />}
                {api.descriptionIssues.length > 0 && <IssueList label="Description Issues" issues={api.descriptionIssues} />}
                {api.titleIssues.length === 0 && api.descriptionIssues.length === 0 && (
                    <div
                        style={{
                            padding: '12px 16px',
                            borderRadius: '8px',
                            backgroundColor: 'hsl(142 71% 45% / 0.1)',
                            color: 'hsl(142 71% 30%)',
                            fontSize: '13px',
                            marginBottom: '20px',
                        }}
                    >
                        <Check size={14} style={{ verticalAlign: 'middle', marginRight: '6px' }} />
                        No issues found. Title and description are well-optimized for discovery.
                    </div>
                )}

                {!suggestion && !loading && (
                    <Button type="button" size="sm" onClick={handleAnalyze} style={{ marginBottom: '20px' }}>
                        <Sparkles size={14} style={{ marginRight: '6px' }} />
                        Generate AI Suggestions
                    </Button>
                )}

                {loading && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '16px 0', color: 'var(--muted-foreground, #6b7280)' }}>
                        <Loader2 size={16} className="animate-spin" style={{ animation: 'spin 1s linear infinite' }} />
                        <span style={{ fontSize: '13px' }}>Generating suggestions with Ollama llama3.2:1b...</span>
                    </div>
                )}

                {error && (
                    <div
                        style={{
                            padding: '12px 16px',
                            borderRadius: '8px',
                            backgroundColor: 'hsl(0 84% 60% / 0.1)',
                            color: 'hsl(0 84% 40%)',
                            fontSize: '13px',
                            marginBottom: '16px',
                        }}
                    >
                        <AlertCircle size={14} style={{ verticalAlign: 'middle', marginRight: '6px' }} />
                        {error}
                    </div>
                )}

                {suggestion && (
                    <div style={{ marginTop: '8px' }}>
                        <h4 style={{ margin: '0 0 12px', fontSize: '14px', fontWeight: 600 }}>AI Suggestions</h4>

                        {suggestion.suggestedTitle && (
                            <SuggestionField label="Suggested Title" current={api.name} suggested={suggestion.suggestedTitle} />
                        )}
                        {suggestion.suggestedDescription && (
                            <SuggestionField label="Suggested Description" current={api.description} suggested={suggestion.suggestedDescription} />
                        )}
                        {suggestion.reasoning && (
                            <div style={{ marginBottom: '16px' }}>
                                <label style={{ fontSize: '12px', fontWeight: 500, color: 'var(--muted-foreground, #6b7280)' }}>Reasoning</label>
                                <p style={{ margin: '4px 0', fontSize: '13px', lineHeight: 1.5 }}>{suggestion.reasoning}</p>
                            </div>
                        )}

                        {!applied && (
                            <div style={{ display: 'flex', gap: '8px', marginTop: '16px' }}>
                                <Button
                                    type="button"
                                    size="sm"
                                    onClick={handleApply}
                                    disabled={applying || !canApply}
                                    title={!canApply ? 'One-click apply is only supported for V4 Proxy APIs' : undefined}
                                >
                                    {applying ? (
                                        <Loader2 size={14} style={{ marginRight: '6px', animation: 'spin 1s linear infinite' }} />
                                    ) : (
                                        <Check size={14} style={{ marginRight: '6px' }} />
                                    )}
                                    Apply Suggestions
                                </Button>
                                {!canApply && (
                                    <span style={{ fontSize: '12px', color: 'var(--muted-foreground, #6b7280)', alignSelf: 'center' }}>
                                        Only V4 Proxy APIs support one-click apply
                                    </span>
                                )}
                            </div>
                        )}

                        {applied && (
                            <div
                                style={{
                                    padding: '12px 16px',
                                    borderRadius: '8px',
                                    backgroundColor: 'hsl(142 71% 45% / 0.1)',
                                    color: 'hsl(142 71% 30%)',
                                    fontSize: '13px',
                                    marginTop: '16px',
                                }}
                            >
                                <Check size={14} style={{ verticalAlign: 'middle', marginRight: '6px' }} />
                                Suggestions applied successfully. Scores will update after re-indexing.
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

function ScoreCard({ label, score, max }: { readonly label: string; readonly score: number; readonly max: number }) {
    const pct = Math.round((score / max) * 100);
    return (
        <div
            style={{
                flex: 1,
                padding: '12px',
                borderRadius: '8px',
                border: '1px solid var(--border, #e5e7eb)',
                textAlign: 'center',
            }}
        >
            <div style={{ fontSize: '11px', color: 'var(--muted-foreground, #6b7280)', marginBottom: '4px' }}>{label}</div>
            <div style={{ fontSize: '20px', fontWeight: 700 }}>
                {score}
                <span style={{ fontSize: '12px', fontWeight: 400, color: 'var(--muted-foreground, #6b7280)' }}>/{max}</span>
            </div>
            <div
                style={{
                    height: '4px',
                    borderRadius: '2px',
                    backgroundColor: 'var(--border, #e5e7eb)',
                    marginTop: '8px',
                    overflow: 'hidden',
                }}
            >
                <div
                    style={{
                        height: '100%',
                        width: `${pct}%`,
                        borderRadius: '2px',
                        backgroundColor: pct >= 70 ? 'hsl(142 71% 45%)' : pct >= 40 ? 'hsl(38 92% 50%)' : 'hsl(0 84% 60%)',
                        transition: 'width 300ms ease',
                    }}
                />
            </div>
        </div>
    );
}

function IssueList({ label, issues }: { readonly label: string; readonly issues: string[] }) {
    return (
        <div style={{ marginBottom: '16px' }}>
            <h5 style={{ margin: '0 0 8px', fontSize: '13px', fontWeight: 600 }}>{label}</h5>
            <ul style={{ margin: 0, padding: 0, listStyle: 'none' }}>
                {issues.map((issue, i) => (
                    <li
                        key={i}
                        style={{
                            display: 'flex',
                            alignItems: 'flex-start',
                            gap: '8px',
                            padding: '6px 0',
                            fontSize: '13px',
                            color: 'var(--muted-foreground, #6b7280)',
                        }}
                    >
                        <AlertCircle size={14} style={{ flexShrink: 0, marginTop: '2px', color: 'hsl(38 92% 50%)' }} />
                        {issue}
                    </li>
                ))}
            </ul>
        </div>
    );
}

function SuggestionField({ label, current, suggested }: { readonly label: string; readonly current: string; readonly suggested: string }) {
    return (
        <div style={{ marginBottom: '16px' }}>
            <label style={{ fontSize: '12px', fontWeight: 500, color: 'var(--muted-foreground, #6b7280)' }}>{label}</label>
            <div
                style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '8px',
                    marginTop: '4px',
                }}
            >
                <div
                    style={{
                        flex: 1,
                        padding: '8px 12px',
                        borderRadius: '6px',
                        backgroundColor: 'hsl(0 84% 60% / 0.05)',
                        border: '1px solid hsl(0 84% 60% / 0.2)',
                        fontSize: '13px',
                        lineHeight: 1.5,
                        textDecoration: 'line-through',
                        color: 'var(--muted-foreground, #6b7280)',
                    }}
                >
                    {current || '(empty)'}
                </div>
                <ArrowRight size={16} style={{ flexShrink: 0, marginTop: '10px', color: 'var(--muted-foreground, #6b7280)' }} />
                <div
                    style={{
                        flex: 1,
                        padding: '8px 12px',
                        borderRadius: '6px',
                        backgroundColor: 'hsl(142 71% 45% / 0.05)',
                        border: '1px solid hsl(142 71% 45% / 0.2)',
                        fontSize: '13px',
                        lineHeight: 1.5,
                    }}
                >
                    {suggested}
                </div>
            </div>
        </div>
    );
}
