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
import { AlertCircle, Check, ChevronDown, ChevronUp, Copy, Loader2, Save, Sparkles, X } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';

import type { ApiQualityScore, ApiSuggestion, ScoreResult } from '../../api/catalog-quality-api';
import { applyApiSuggestion, fetchApiSuggestions, fetchScore } from '../../api/catalog-quality-api';
import { ScoreBadge } from './ScoreBadge';

interface AnalysisPanelProps {
    readonly api: ApiQualityScore;
    readonly envId: string;
    readonly onClose: () => void;
    readonly onApplied: () => void;
}

const inputStyle: React.CSSProperties = {
    width: '100%',
    padding: '8px 12px',
    borderRadius: '6px',
    border: '1px solid var(--border, #e5e7eb)',
    fontSize: '14px',
    fontFamily: 'inherit',
    backgroundColor: 'var(--background, #fff)',
    color: 'var(--foreground, #111827)',
    outline: 'none',
    boxSizing: 'border-box',
};

const textareaStyle: React.CSSProperties = {
    ...inputStyle,
    minHeight: '100px',
    resize: 'vertical',
    lineHeight: 1.5,
};

const labelStyle: React.CSSProperties = {
    display: 'block',
    fontSize: '12px',
    fontWeight: 600,
    color: 'var(--muted-foreground, #6b7280)',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    marginBottom: '6px',
};

export function AnalysisPanel({ api, envId, onClose, onApplied }: AnalysisPanelProps) {
    const [editTitle, setEditTitle] = useState(api.name || '');
    const [editDescription, setEditDescription] = useState(api.description || '');

    const [liveScore, setLiveScore] = useState<ScoreResult>({
        titleScore: api.titleScore,
        descriptionScore: api.descriptionScore,
        totalScore: api.totalScore,
        titleIssues: api.titleIssues,
        descriptionIssues: api.descriptionIssues,
    });
    const [scoring, setScoring] = useState(false);

    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [saved, setSaved] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [suggestion, setSuggestion] = useState<ApiSuggestion | null>(null);
    const [reasoningOpen, setReasoningOpen] = useState(false);

    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const abortRef = useRef<AbortController | null>(null);

    const originalTitle = api.name || '';
    const originalDescription = api.description || '';
    const hasChanges = editTitle !== originalTitle || editDescription !== originalDescription;
    const canSave = api.definitionVersion === 'V4' && api.apiType === 'PROXY';
    const scoreDelta = liveScore.totalScore - api.totalScore;

    useEffect(() => {
        setEditTitle(api.name || '');
        setEditDescription(api.description || '');
        setLiveScore({
            titleScore: api.titleScore,
            descriptionScore: api.descriptionScore,
            totalScore: api.totalScore,
            titleIssues: api.titleIssues,
            descriptionIssues: api.descriptionIssues,
        });
        setSaved(false);
        setSuggestion(null);
        setError(null);
    }, [api.apiId, api.name, api.description, api.titleScore, api.descriptionScore, api.totalScore, api.titleIssues, api.descriptionIssues]);

    const runScore = useCallback(
        (title: string, description: string) => {
            if (debounceRef.current) clearTimeout(debounceRef.current);
            if (abortRef.current) abortRef.current.abort();

            debounceRef.current = setTimeout(async () => {
                const controller = new AbortController();
                abortRef.current = controller;
                setScoring(true);
                try {
                    const result = await fetchScore(envId, title, description);
                    if (!controller.signal.aborted) {
                        setLiveScore(result);
                    }
                } catch {
                    // Silently ignore scoring errors — the user can still type
                } finally {
                    if (!controller.signal.aborted) {
                        setScoring(false);
                    }
                }
            }, 300);
        },
        [envId],
    );

    const handleTitleChange = useCallback(
        (value: string) => {
            setEditTitle(value);
            setSaved(false);
            runScore(value, editDescription);
        },
        [editDescription, runScore],
    );

    const handleDescriptionChange = useCallback(
        (value: string) => {
            setEditDescription(value);
            setSaved(false);
            runScore(editTitle, value);
        },
        [editTitle, runScore],
    );

    const handleGenerateAI = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const result = await fetchApiSuggestions(api.apiId, envId);
            setSuggestion(result);
            setReasoningOpen(false);
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to generate suggestions');
        } finally {
            setLoading(false);
        }
    }, [api.apiId, envId]);

    const handleApplySuggestion = useCallback(() => {
        if (!suggestion) return;
        if (suggestion.suggestedTitle) setEditTitle(suggestion.suggestedTitle);
        if (suggestion.suggestedDescription) setEditDescription(suggestion.suggestedDescription);
        setSaved(false);
        runScore(suggestion.suggestedTitle || editTitle, suggestion.suggestedDescription || editDescription);
    }, [suggestion, editTitle, editDescription, runScore]);

    const handleSave = useCallback(async () => {
        setSaving(true);
        setError(null);
        try {
            await applyApiSuggestion(api.apiId, envId, editTitle, editDescription);
            setSaved(true);
            onApplied();
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to save changes');
        } finally {
            setSaving(false);
        }
    }, [api.apiId, envId, editTitle, editDescription, onApplied]);

    useEffect(() => {
        return () => {
            if (debounceRef.current) clearTimeout(debounceRef.current);
            if (abortRef.current) abortRef.current.abort();
        };
    }, []);

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
            {/* Header */}
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '16px 20px',
                    borderBottom: '1px solid var(--border, #e5e7eb)',
                }}
            >
                <div>
                    <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>Quality Analysis</h3>
                    <span style={{ fontSize: '12px', color: 'var(--muted-foreground, #6b7280)' }}>
                        {api.definitionVersion && `${api.definitionVersion}`}
                        {api.apiType && ` · ${api.apiType}`}
                    </span>
                </div>
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

            {/* Scrollable content */}
            <div style={{ flex: 1, overflow: 'auto', padding: '20px' }}>
                {/* Title field */}
                <div style={{ marginBottom: '20px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                        <label style={labelStyle}>Title</label>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <ScoreBadge score={liveScore.titleScore} max={50} />
                            <span style={{ fontSize: '11px', color: 'var(--muted-foreground, #6b7280)' }}>/50</span>
                        </div>
                    </div>
                    <input
                        type="text"
                        value={editTitle}
                        onChange={(e) => handleTitleChange(e.target.value)}
                        placeholder="Enter API title…"
                        style={inputStyle}
                    />
                    {liveScore.titleIssues.length > 0 && <IssueList issues={liveScore.titleIssues} />}
                </div>

                {/* Description field */}
                <div style={{ marginBottom: '20px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                        <label style={labelStyle}>Description</label>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <ScoreBadge score={liveScore.descriptionScore} max={50} />
                            <span style={{ fontSize: '11px', color: 'var(--muted-foreground, #6b7280)' }}>/50</span>
                        </div>
                    </div>
                    <textarea
                        value={editDescription}
                        onChange={(e) => handleDescriptionChange(e.target.value)}
                        placeholder="Enter API description…"
                        style={textareaStyle}
                    />
                    {liveScore.descriptionIssues.length > 0 && <IssueList issues={liveScore.descriptionIssues} />}
                </div>

                {/* Overall score with delta */}
                <div
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '12px 16px',
                        borderRadius: '8px',
                        border: '1px solid var(--border, #e5e7eb)',
                        marginBottom: '20px',
                    }}
                >
                    <span style={{ fontSize: '13px', fontWeight: 600 }}>Overall Score</span>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        {scoring && <Loader2 size={14} style={{ animation: 'spin 1s linear infinite', color: 'var(--muted-foreground, #6b7280)' }} />}
                        <ScoreBadge score={liveScore.totalScore} />
                        <span style={{ fontSize: '11px', color: 'var(--muted-foreground, #6b7280)' }}>/100</span>
                        {scoreDelta !== 0 && (
                            <span
                                style={{
                                    fontSize: '13px',
                                    fontWeight: 600,
                                    color: scoreDelta > 0 ? 'hsl(142 71% 35%)' : 'hsl(0 84% 50%)',
                                }}
                            >
                                {scoreDelta > 0 ? '+' : ''}
                                {scoreDelta}
                            </span>
                        )}
                    </div>
                </div>

                {/* No issues banner */}
                {liveScore.titleIssues.length === 0 && liveScore.descriptionIssues.length === 0 && (
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

                {/* AI Suggestions section */}
                <div style={{ marginBottom: '20px' }}>
                    <Button type="button" variant="outline" size="sm" onClick={handleGenerateAI} disabled={loading}>
                        {loading ? (
                            <Loader2 size={14} style={{ marginRight: '6px', animation: 'spin 1s linear infinite' }} />
                        ) : (
                            <Sparkles size={14} style={{ marginRight: '6px' }} />
                        )}
                        {loading ? 'Generating…' : 'Generate AI Suggestions'}
                    </Button>
                </div>

                {/* AI Suggestion results card */}
                {suggestion && (
                    <div
                        style={{
                            marginBottom: '20px',
                            border: '1px solid hsl(262 80% 60% / 0.2)',
                            borderRadius: '8px',
                            overflow: 'hidden',
                        }}
                    >
                        <div
                            style={{
                                padding: '10px 14px',
                                backgroundColor: 'hsl(262 80% 60% / 0.06)',
                                borderBottom: '1px solid hsl(262 80% 60% / 0.12)',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '6px',
                            }}
                        >
                            <Sparkles size={14} style={{ color: 'hsl(262 80% 50%)' }} />
                            <span style={{ fontSize: '12px', fontWeight: 600, color: 'hsl(262 80% 40%)' }}>AI Suggestions</span>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={handleApplySuggestion}
                                style={{ marginLeft: 'auto', fontSize: '11px', padding: '2px 10px', height: 'auto' }}
                            >
                                <Check size={12} style={{ marginRight: '4px' }} />
                                Apply to fields
                            </Button>
                        </div>
                        <div style={{ padding: '14px' }}>
                            {suggestion.suggestedTitle && (
                                <CopyableField label="Suggested Title" value={suggestion.suggestedTitle} />
                            )}
                            {suggestion.suggestedDescription && (
                                <CopyableField label="Suggested Description" value={suggestion.suggestedDescription} />
                            )}
                            {suggestion.reasoning && (
                                <div style={{ marginTop: suggestion.suggestedTitle || suggestion.suggestedDescription ? '12px' : 0 }}>
                                    <button
                                        onClick={() => setReasoningOpen(!reasoningOpen)}
                                        style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '4px',
                                            border: 'none',
                                            background: 'none',
                                            cursor: 'pointer',
                                            padding: '4px 0',
                                            fontSize: '12px',
                                            fontWeight: 500,
                                            color: 'var(--muted-foreground, #6b7280)',
                                        }}
                                    >
                                        {reasoningOpen ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
                                        Why these suggestions?
                                    </button>
                                    {reasoningOpen && (
                                        <p
                                            style={{
                                                margin: '8px 0 0',
                                                padding: '10px 12px',
                                                borderRadius: '6px',
                                                backgroundColor: 'var(--accent, #f3f4f6)',
                                                fontSize: '13px',
                                                lineHeight: 1.6,
                                                color: 'var(--muted-foreground, #6b7280)',
                                            }}
                                        >
                                            {suggestion.reasoning}
                                        </p>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {/* Error */}
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

                {/* Saved confirmation */}
                {saved && (
                    <div
                        style={{
                            padding: '12px 16px',
                            borderRadius: '8px',
                            backgroundColor: 'hsl(142 71% 45% / 0.1)',
                            color: 'hsl(142 71% 30%)',
                            fontSize: '13px',
                            marginBottom: '16px',
                        }}
                    >
                        <Check size={14} style={{ verticalAlign: 'middle', marginRight: '6px' }} />
                        Changes saved successfully. Scores will update after re-indexing.
                    </div>
                )}
            </div>

            {/* Sticky bottom action bar */}
            <div
                style={{
                    padding: '12px 20px',
                    borderTop: '1px solid var(--border, #e5e7eb)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                }}
            >
                <Button
                    type="button"
                    size="sm"
                    onClick={handleSave}
                    disabled={!hasChanges || saving || !canSave}
                    title={!canSave ? 'Saving is only supported for V4 Proxy APIs' : !hasChanges ? 'No changes to save' : undefined}
                >
                    {saving ? (
                        <Loader2 size={14} style={{ marginRight: '6px', animation: 'spin 1s linear infinite' }} />
                    ) : (
                        <Save size={14} style={{ marginRight: '6px' }} />
                    )}
                    Save Changes
                </Button>
                {!canSave && hasChanges && (
                    <span style={{ fontSize: '12px', color: 'var(--muted-foreground, #6b7280)' }}>Only V4 Proxy APIs support saving</span>
                )}
            </div>
        </div>
    );
}

function CopyableField({ label, value }: { readonly label: string; readonly value: string }) {
    const [copied, setCopied] = useState(false);

    const handleCopy = useCallback(() => {
        navigator.clipboard.writeText(value).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        });
    }, [value]);

    return (
        <div style={{ marginBottom: '10px' }}>
            <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--muted-foreground, #6b7280)', marginBottom: '4px' }}>{label}</div>
            <div
                style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '8px',
                    padding: '8px 10px',
                    borderRadius: '6px',
                    backgroundColor: 'var(--accent, #f9fafb)',
                    border: '1px solid var(--border, #e5e7eb)',
                }}
            >
                <span style={{ flex: 1, fontSize: '13px', lineHeight: 1.5, wordBreak: 'break-word' }}>{value}</span>
                <button
                    onClick={handleCopy}
                    title="Copy to clipboard"
                    style={{
                        flexShrink: 0,
                        border: 'none',
                        background: 'none',
                        cursor: 'pointer',
                        padding: '2px',
                        borderRadius: '4px',
                        display: 'flex',
                        color: copied ? 'hsl(142 71% 40%)' : 'var(--muted-foreground, #6b7280)',
                    }}
                >
                    {copied ? <Check size={14} /> : <Copy size={14} />}
                </button>
            </div>
        </div>
    );
}

function IssueList({ issues }: { readonly issues: string[] }) {
    return (
        <ul style={{ margin: '8px 0 0', padding: 0, listStyle: 'none' }}>
            {issues.map((issue, i) => (
                <li
                    key={i}
                    style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '6px',
                        padding: '4px 0',
                        fontSize: '12px',
                        color: 'var(--muted-foreground, #6b7280)',
                    }}
                >
                    <AlertCircle size={12} style={{ flexShrink: 0, marginTop: '2px', color: 'hsl(38 92% 50%)' }} />
                    {issue}
                </li>
            ))}
        </ul>
    );
}
