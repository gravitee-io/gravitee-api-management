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
    Alert,
    AlertDescription,
    AlertTitle,
    Badge,
    Button,
    Collapsible,
    CollapsibleContent,
    Separator,
    cn,
} from '@gravitee/graphene-core';
import {
    ArrowDownIcon,
    ArrowLeftIcon,
    ArrowRightIcon,
    ChevronDownIcon,
    LayoutGridIcon,
    PencilIcon,
    TriangleAlertIcon,
} from '@gravitee/graphene-core/icons';
import type { CSSProperties, ReactNode } from 'react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { PROXY_TEMPLATES, TEMPLATE_COLOR_STYLES } from '../features/apis/templates/proxyTemplates';

// ─── Flow diagram primitives ─────────────────────────────────────────────────

/*
 * Grid layout: 1fr auto 1fr auto 1fr
 * - Client and Your Backend fill equal space on each side (no blank gap)
 * - Gateway is centred and owns a flex-col for secondary elements below it
 * - Arrows are auto-width; marginTop centres them with the node height (~25 px)
 */
const FLOW_GRID: CSSProperties = {
    display: 'grid',
    gridTemplateColumns: '1fr auto 1fr auto 1fr',
    gap: '6px',
    alignItems: 'start',
};
const ARROW_ALIGN: CSSProperties = { marginTop: '6px' };

// Inline styles for colours not expressible as Tailwind semantic tokens
const NODE_STYLE = {
    blue: { backgroundColor: 'rgba(59,130,246,0.1)', color: '#3b82f6', borderColor: 'rgba(59,130,246,0.35)' },
    violet: { backgroundColor: 'rgba(139,92,246,0.1)', color: '#8b5cf6', borderColor: 'rgba(139,92,246,0.35)' },
    amber: { backgroundColor: 'rgba(245,158,11,0.1)', color: '#d97706', borderColor: 'rgba(245,158,11,0.35)' },
} as const;

function FlowNode({ label, variant = 'neutral' }: { label: string; variant?: 'neutral' | 'success' | 'blue' | 'violet' | 'amber' }) {
    const base = 'rounded border px-2 py-1 text-xs font-medium whitespace-nowrap text-center';
    if (variant === 'neutral') return <div className={`${base} bg-muted text-muted-foreground border-border`}>{label}</div>;
    if (variant === 'success') return <div className={`${base} bg-success/5 text-success border-success/20`}>{label}</div>;
    return (
        <div className={base} style={NODE_STYLE[variant]}>
            {label}
        </div>
    );
}

function FlowLabel({ children }: { children: React.ReactNode }) {
    return (
        <p className="font-semibold uppercase text-muted-foreground" style={{ fontSize: '9px', letterSpacing: '0.06em' }}>
            {children}
        </p>
    );
}

function ApiKeyFlow() {
    return (
        <div className="rounded-lg border bg-muted/30 p-3 space-y-2">
            <FlowLabel>Request flow</FlowLabel>
            <div style={FLOW_GRID}>
                <div className="flex justify-center">
                    <FlowNode label="Client" />
                </div>
                <ArrowRightIcon className="size-3 text-muted-foreground shrink-0" style={ARROW_ALIGN} aria-hidden />
                <div className="flex flex-col items-center gap-1">
                    <FlowNode label="Gateway" variant="blue" />
                    <span
                        className="rounded font-mono"
                        style={{ backgroundColor: 'rgba(59,130,246,0.12)', color: '#3b82f6', fontSize: '9px', padding: '1px 5px' }}
                    >
                        API Key check
                    </span>
                </div>
                <ArrowRightIcon className="size-3 text-muted-foreground shrink-0" style={ARROW_ALIGN} aria-hidden />
                <div className="flex justify-center">
                    <FlowNode label="Your Backend" variant="success" />
                </div>
            </div>
        </div>
    );
}

function JwtFlow() {
    return (
        <div className="rounded-lg border bg-muted/30 p-3 space-y-2">
            <FlowLabel>Request flow</FlowLabel>
            <div style={FLOW_GRID}>
                <div className="flex justify-center">
                    <FlowNode label="Client" />
                </div>
                <ArrowRightIcon className="size-3 text-muted-foreground shrink-0" style={ARROW_ALIGN} aria-hidden />
                <div className="flex flex-col items-center gap-1">
                    <FlowNode label="Gateway" variant="violet" />
                    <ArrowDownIcon className="size-3 text-muted-foreground" aria-hidden />
                    <FlowNode label="Identity Provider" variant="amber" />
                </div>
                <ArrowRightIcon className="size-3 text-muted-foreground shrink-0" style={ARROW_ALIGN} aria-hidden />
                <div className="flex justify-center">
                    <FlowNode label="Your Backend" variant="success" />
                </div>
            </div>
            <p className="text-muted-foreground" style={{ fontSize: '9px' }}>
                JWT validated via JWKS endpoint
            </p>
        </div>
    );
}

function OAuthFlow() {
    return (
        <div className="rounded-lg border bg-muted/30 p-3 space-y-2">
            <FlowLabel>Request flow</FlowLabel>
            <div style={FLOW_GRID}>
                <div className="flex justify-center">
                    <FlowNode label="Client" />
                </div>
                <ArrowRightIcon className="size-3 text-muted-foreground shrink-0" style={ARROW_ALIGN} aria-hidden />
                <div className="flex flex-col items-center gap-1">
                    <FlowNode label="Gateway" variant="amber" />
                    <ArrowDownIcon className="size-3 text-muted-foreground" aria-hidden />
                    <FlowNode label="Auth Server" variant="violet" />
                </div>
                <ArrowRightIcon className="size-3 text-muted-foreground shrink-0" style={ARROW_ALIGN} aria-hidden />
                <div className="flex justify-center">
                    <FlowNode label="Your Backend" variant="success" />
                </div>
            </div>
            <p className="text-muted-foreground" style={{ fontSize: '9px' }}>
                OAuth 2.0 token introspection
            </p>
        </div>
    );
}

function KeylessFlow() {
    return (
        <div className="rounded-lg border bg-muted/30 p-3 space-y-2">
            <FlowLabel>Request flow</FlowLabel>
            <div style={FLOW_GRID}>
                <div className="flex justify-center">
                    <FlowNode label="Client" />
                </div>
                <ArrowRightIcon className="size-3 text-muted-foreground shrink-0" style={ARROW_ALIGN} aria-hidden />
                <div className="flex flex-col items-center gap-1">
                    <FlowNode label="Gateway" variant="success" />
                    <span
                        className="rounded font-mono"
                        style={{ backgroundColor: 'rgba(245,158,11,0.15)', color: '#d97706', fontSize: '9px', padding: '1px 5px' }}
                    >
                        No credentials
                    </span>
                </div>
                <ArrowRightIcon className="size-3 text-muted-foreground shrink-0" style={ARROW_ALIGN} aria-hidden />
                <div className="flex justify-center">
                    <FlowNode label="Your Backend" variant="success" />
                </div>
            </div>
        </div>
    );
}

// ─── Flow component map ───────────────────────────────────────────────────────

const FLOW_COMPONENTS: Partial<Record<string, ReactNode>> = {
    'rest-api-key': <ApiKeyFlow />,
    'rest-jwt': <JwtFlow />,
    'rest-oauth2': <OAuthFlow />,
    'rest-keyless': <KeylessFlow />,
};

// ─── Page component ───────────────────────────────────────────────────────────

export function CreateApiProxyPage() {
    const navigate = useNavigate();
    const [templatesOpen, setTemplatesOpen] = useState(false);
    const [hoveredCardId, setHoveredCardId] = useState<string | null>(null);

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Create API Proxy</h1>
                <p className="text-sm text-muted-foreground">Choose the full wizard or a preset, then add your API details.</p>
            </div>

            {/* Picker cards */}
            <div className="grid grid-cols-2 gap-4">
                {/* Start from scratch */}
                <button
                    type="button"
                    onClick={() => navigate('scratch')}
                    className="group flex flex-col gap-4 rounded-xl border-2 border-primary/30 bg-primary/5 p-6 text-left transition-all hover:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
                    style={{ minHeight: '11rem', borderRadius: '0.75rem', boxShadow: '0 1px 4px 0 rgba(0,0,0,0.07)' }}
                >
                    <div className="flex items-start gap-4">
                        <div className="rounded-lg bg-primary/10 p-3 shrink-0">
                            <PencilIcon className="size-6 text-primary" aria-hidden />
                        </div>
                        <div className="min-w-0 flex-1 space-y-1">
                            <p className="text-base font-semibold text-foreground">Start from scratch</p>
                            <p className="text-sm text-muted-foreground leading-relaxed">
                                Full four-step wizard: details, entrypoints, security, and review. No preset plans — you choose everything
                                explicitly.
                            </p>
                        </div>
                        <ArrowRightIcon
                            className="size-5 shrink-0 text-primary mt-0.5 transition-transform group-hover:translate-x-0.5"
                            aria-hidden
                        />
                    </div>
                </button>

                {/* Quick-start templates */}
                <button
                    type="button"
                    onClick={() => setTemplatesOpen(o => !o)}
                    className={cn(
                        'group flex flex-col gap-4 rounded-xl border-2 bg-card p-6 text-left transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40',
                        templatesOpen ? 'border-primary/50 bg-primary/5' : 'border-border hover:border-primary hover:bg-muted',
                    )}
                    style={{ minHeight: '11rem', borderRadius: '0.75rem', boxShadow: '0 1px 4px 0 rgba(0,0,0,0.07)' }}
                >
                    <div className="flex items-start gap-4">
                        <div className="rounded-lg bg-muted p-3 shrink-0">
                            <LayoutGridIcon className="size-6 text-primary" aria-hidden />
                        </div>
                        <div className="min-w-0 flex-1 space-y-1">
                            <p className="text-base font-semibold text-foreground">Quick-start templates</p>
                            <p className="text-sm text-muted-foreground leading-relaxed">
                                Presets with security and plans filled in. You mainly add name and upstream URL; adjust anything on review.
                            </p>
                        </div>
                        <ChevronDownIcon
                            className={cn(
                                'size-5 shrink-0 text-muted-foreground mt-0.5 transition-transform duration-200',
                                templatesOpen && 'rotate-180',
                            )}
                            aria-hidden
                        />
                    </div>
                </button>
            </div>

            {/* Collapsible template cards */}
            <Collapsible open={templatesOpen} onOpenChange={setTemplatesOpen}>
                <CollapsibleContent>
                    <div className="space-y-4 rounded-xl border border-dashed bg-muted/30 p-5" style={{ borderRadius: '0.75rem' }}>
                        <p className="text-sm text-muted-foreground">
                            Pick a template to open essentials with the right auth pattern pre-selected.
                        </p>
                        <div className="grid grid-cols-2 gap-4">
                            {PROXY_TEMPLATES.map(tpl => {
                                const Icon = tpl.Icon;
                                return (
                                    <button
                                        key={tpl.id}
                                        type="button"
                                        onClick={() => navigate(`template/${tpl.id}`)}
                                        className={cn(
                                            'flex flex-col gap-3 rounded-xl border bg-card p-5 text-left transition-all',
                                            tpl.notRecommended ? undefined : 'border-border hover:border-primary',
                                        )}
                                        style={{
                                            borderRadius: '0.75rem',
                                            boxShadow: '0 1px 3px 0 rgba(0,0,0,0.06)',
                                            ...(tpl.notRecommended && {
                                                borderColor: hoveredCardId === tpl.id ? 'rgba(245,158,11,0.4)' : 'rgba(245,158,11,0.25)',
                                            }),
                                        }}
                                        onMouseEnter={() => setHoveredCardId(tpl.id)}
                                        onMouseLeave={() => setHoveredCardId(null)}
                                    >
                                        {/* Card header */}
                                        <div className="flex items-start gap-3">
                                            <div className="rounded-lg p-2.5 shrink-0" style={TEMPLATE_COLOR_STYLES[tpl.color].iconBg}>
                                                <Icon className="size-5" style={TEMPLATE_COLOR_STYLES[tpl.color].iconColor} aria-hidden />
                                            </div>
                                            <div className="min-w-0 flex-1 space-y-0.5">
                                                <div className="flex flex-wrap items-center gap-2">
                                                    <p className="text-sm font-medium leading-snug">{tpl.title}</p>
                                                    {tpl.notRecommended && (
                                                        <Badge
                                                            variant="outline"
                                                            style={{
                                                                fontSize: '10px',
                                                                color: '#92400e',
                                                                borderColor: 'rgba(245,158,11,0.5)',
                                                                backgroundColor: 'rgba(245,158,11,0.1)',
                                                            }}
                                                        >
                                                            Not recommended
                                                        </Badge>
                                                    )}
                                                </div>
                                                <p className="text-xs text-muted-foreground">{tpl.subtitle}</p>
                                            </div>
                                        </div>

                                        {/* Description */}
                                        <p className="text-xs leading-relaxed text-muted-foreground">{tpl.description}</p>

                                        {/* Warning for keyless */}
                                        {tpl.notRecommended && tpl.warningMessage && (
                                            <Alert
                                                className="py-2"
                                                style={{ borderColor: 'rgba(245,158,11,0.4)', backgroundColor: 'rgba(245,158,11,0.05)' }}
                                            >
                                                <TriangleAlertIcon className="size-4" style={{ color: '#d97706' }} aria-hidden />
                                                <AlertTitle className="text-xs font-semibold" style={{ color: '#92400e' }}>
                                                    Demo and testing only
                                                </AlertTitle>
                                                <AlertDescription
                                                    className="text-xs leading-snug"
                                                    style={{ color: 'rgba(120,53,15,0.85)' }}
                                                >
                                                    {tpl.warningMessage}
                                                </AlertDescription>
                                            </Alert>
                                        )}

                                        {/* Request flow diagram */}
                                        {FLOW_COMPONENTS[tpl.id] ?? null}

                                        {/* Tags */}
                                        <div className="flex flex-wrap gap-1.5">
                                            {tpl.tags.map(tag => (
                                                <span key={tag} className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                                                    {tag}
                                                </span>
                                            ))}
                                        </div>
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                </CollapsibleContent>
            </Collapsible>

            {/* Navigation bar */}
            <Separator />
            <div className="flex items-center justify-between">
                <Button variant="outline" onClick={() => navigate('..')}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Cancel
                </Button>
                <p className="text-xs text-muted-foreground">
                    Pick <span className="font-medium text-foreground">Start from scratch</span> or select a template preset.
                </p>
            </div>
        </div>
    );
}
