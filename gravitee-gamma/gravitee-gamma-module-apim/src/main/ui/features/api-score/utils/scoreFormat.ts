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
import type { EnvironmentApiScore, ScoreSummary, ScoringAssetFormat, ScoringSeverityKey } from '../types/apiScore';

/**
 * Single source of truth for all API-score display formatting: score/severity thresholds,
 * badge color classes, and ruleset-format labels/options. No literals scattered in components.
 */

// ─── Score ────────────────────────────────────────────────────────────────────

/** Backend `score` is a 0..1 fraction; missing means "never evaluated". */
export const SCORE_SUCCESS_THRESHOLD = 0.8;
export const SCORE_WARNING_THRESHOLD = 0.4;

export function scoreToPercent(score: number | null | undefined): number | null {
    if (typeof score !== 'number' || score < 0) return null;
    return Math.round(score * 100);
}

/** Tailwind text color for a score, by threshold. */
export function scoreTextColor(score: number | null | undefined): string {
    if (typeof score !== 'number' || score < 0) return 'text-muted-foreground';
    if (score >= SCORE_SUCCESS_THRESHOLD) return 'text-success';
    if (score >= SCORE_WARNING_THRESHOLD) return 'text-warning';
    return 'text-destructive';
}

/** Matching border color for a score badge. */
function scoreBorderColor(score: number | null | undefined): string {
    if (typeof score !== 'number' || score < 0) return 'border-border';
    if (score >= SCORE_SUCCESS_THRESHOLD) return 'border-success/20';
    if (score >= SCORE_WARNING_THRESHOLD) return 'border-warning/30';
    return 'border-destructive/30';
}

/** Tailwind text/border classes for a score badge, matching the SyncStatusBadge convention. */
export function scoreColorClasses(score: number | null | undefined): string {
    return `${scoreTextColor(score)} ${scoreBorderColor(score)}`;
}

/**
 * Aggregates the overview cards from a set of scored APIs. Used because `/scoring/apis` is filtered
 * to V4 HTTP-proxy APIs client-side, so the environment-wide `/scoring/overview` no longer matches.
 * The average score is the mean over APIs that have actually been scored; null when none have.
 */
export function deriveScoreSummary(apis: EnvironmentApiScore[]): ScoreSummary {
    const scored = apis.filter((api): api is EnvironmentApiScore & { score: number } => typeof api.score === 'number' && api.score >= 0);
    const score = scored.length ? scored.reduce((sum, api) => sum + api.score, 0) / scored.length : null;
    const total = (key: ScoringSeverityKey) => scored.reduce((sum, api) => sum + (api[key] ?? 0), 0);
    return { score, errors: total('errors'), warnings: total('warnings'), infos: total('infos'), hints: total('hints') };
}

/** Overview cards: errors → warnings → hints → infos (classic dashboard order). */
export const OVERVIEW_SEVERITY_CARDS: { key: ScoringSeverityKey; label: string }[] = [
    { key: 'errors', label: 'Errors' },
    { key: 'warnings', label: 'Warnings' },
    { key: 'hints', label: 'Hints' },
    { key: 'infos', label: 'Infos' },
];

/** Overview table: errors → warnings → infos → hints (classic table order). */
export const TABLE_SEVERITY_COLUMNS: { key: ScoringSeverityKey; label: string }[] = [
    { key: 'errors', label: 'Errors' },
    { key: 'warnings', label: 'Warnings' },
    { key: 'infos', label: 'Infos' },
    { key: 'hints', label: 'Hints' },
];

// ─── Severity counts ──────────────────────────────────────────────────────────

/** Per-severity text color; a `0` count renders neutral, `>0` renders the severity color. */
const SEVERITY_TEXT_CLASS: Record<ScoringSeverityKey, string> = {
    errors: 'text-destructive border-destructive/30',
    warnings: 'text-warning border-warning/30',
    infos: 'text-primary border-primary/30',
    hints: 'text-success border-success/20',
};

const NEUTRAL_COUNT_CLASS = 'text-muted-foreground border-border';

export function countColorClasses(severity: ScoringSeverityKey, count: number | null | undefined): string {
    if (typeof count !== 'number') return NEUTRAL_COUNT_CLASS;
    return count > 0 ? SEVERITY_TEXT_CLASS[severity] : NEUTRAL_COUNT_CLASS;
}

// ─── Ruleset format labels & import options ────────────────────────────────────

const RULESET_FORMAT_LABELS: Record<ScoringAssetFormat, string> = {
    OPENAPI: 'OpenAPI',
    ASYNCAPI: 'AsyncAPI',
    GRAVITEE_PROXY: 'Gravitee Proxy API',
    GRAVITEE_MESSAGE: 'Gravitee Message API',
    GRAVITEE_NATIVE: 'Native Kafka',
    GRAVITEE_FEDERATION: 'Gravitee Federated API',
    GRAVITEE_V2: 'Gravitee V2 API',
};

export function rulesetFormatLabel(format: ScoringAssetFormat | undefined): string | undefined {
    return format ? RULESET_FORMAT_LABELS[format] : undefined;
}

/** Top-level asset-format choice in the import sheet (mirrors the classic console cards). */
export type RulesetImportKind = 'OPENAPI' | 'ASYNCAPI' | 'GRAVITEE';

export const RULESET_IMPORT_KIND_OPTIONS: { value: RulesetImportKind; label: string }[] = [
    { value: 'OPENAPI', label: 'OpenAPI' },
    { value: 'ASYNCAPI', label: 'AsyncAPI' },
    { value: 'GRAVITEE', label: 'Gravitee API' },
];

/** Gravitee-API sub-formats revealed when "Gravitee API" is picked. */
export const GRAVITEE_FORMAT_OPTIONS: { value: ScoringAssetFormat; label: string }[] = [
    { value: 'GRAVITEE_PROXY', label: RULESET_FORMAT_LABELS.GRAVITEE_PROXY },
    { value: 'GRAVITEE_MESSAGE', label: RULESET_FORMAT_LABELS.GRAVITEE_MESSAGE },
    { value: 'GRAVITEE_NATIVE', label: RULESET_FORMAT_LABELS.GRAVITEE_NATIVE },
    { value: 'GRAVITEE_FEDERATION', label: RULESET_FORMAT_LABELS.GRAVITEE_FEDERATION },
    { value: 'GRAVITEE_V2', label: RULESET_FORMAT_LABELS.GRAVITEE_V2 },
];

/** Resolve the concrete `ScoringAssetFormat` sent to the API from the two-step import selection. */
export function resolveImportFormat(kind: RulesetImportKind, graviteeFormat: ScoringAssetFormat | null): ScoringAssetFormat | null {
    if (kind === 'OPENAPI') return 'OPENAPI';
    if (kind === 'ASYNCAPI') return 'ASYNCAPI';
    return graviteeFormat;
}

// ─── Validation constants (mirror classic) ─────────────────────────────────────

export const RULESET_NAME_MAX = 50;
export const RULESET_DESCRIPTION_MAX = 250;
export const RULESET_FILE_EXTENSIONS = ['yml', 'yaml', 'json'] as const;
export const FUNCTION_NAME_MAX = 50;
export const FUNCTION_FILE_EXTENSIONS = ['js'] as const;
/** A function name must be a bare `<name>.js` (no path separators). */
export const FUNCTION_NAME_PATTERN = /^[^/]+\.js$/;
