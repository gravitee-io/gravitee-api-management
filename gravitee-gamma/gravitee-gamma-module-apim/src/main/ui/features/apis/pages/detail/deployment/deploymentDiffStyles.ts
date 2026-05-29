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

/** Preserves row height and fixed-width gutter columns when a pane has no text. */
export const DIFF_NBSP = '\u00a0';

/** Graphene semantic tones for deployment definition diffs (never raw red/green palettes). */
export type DiffPaneTone = 'neutral' | 'removed' | 'added' | 'removed-gap' | 'added-gap';

export type DiffPaneSide = 'left' | 'right';

export type DiffPaneVariant = 'unchanged' | 'changed' | 'placeholder';

export const DEPLOYMENT_DIFF = {
    table: 'w-full table-fixed border-collapse font-mono text-xs',
    headerRow: 'border-b bg-muted/20',
    headerCell: 'px-0 py-0 text-left font-normal border-r border-border',
    bodyCell: 'px-0 py-0 align-top',
    bodyCellDivider: 'border-r border-border',
    versionBadge: 'text-xs font-mono px-1.5 py-0',
    pane: {
        row: 'flex items-stretch min-h-5',
        lineNum: 'select-none shrink-0 w-10 text-right pr-2 pl-1 tabular-nums text-xs leading-5 border-r',
        gutter: 'select-none shrink-0 w-5 text-center text-xs leading-5 font-semibold border-r',
        content: 'flex-1 px-3 text-xs leading-5 overflow-hidden text-ellipsis',
    },
    tone: {
        neutral: {
            lineNum: 'bg-muted/40 text-muted-foreground/50 border-border/50',
            gutter: 'bg-muted/40 text-transparent border-border/50',
            content: '',
        },
        removed: {
            lineNum: 'bg-destructive/15 text-destructive border-destructive/25',
            gutter: 'bg-destructive/20 text-destructive border-destructive/30',
            content: 'bg-destructive/5 text-foreground',
        },
        added: {
            lineNum: 'bg-success/15 text-success border-success/25',
            gutter: 'bg-success/20 text-success border-success/30',
            content: 'bg-success/5 text-foreground',
        },
        'removed-gap': {
            lineNum: 'bg-destructive/5 text-transparent border-border/50',
            gutter: 'bg-destructive/5 text-transparent border-border/50',
            content: 'bg-destructive/5',
        },
        'added-gap': {
            lineNum: 'bg-success/5 text-transparent border-border/50',
            gutter: 'bg-success/5 text-transparent border-border/50',
            content: 'bg-success/5',
        },
    },
    unified: {
        lineNum: 'select-none text-right pr-3 pl-2 tabular-nums text-xs leading-5 align-top border-r',
        gutter: 'select-none text-center text-xs leading-5 align-top border-r font-semibold w-5',
        content: 'px-3 py-0 leading-5 whitespace-pre align-top',
    },
} as const;

export function resolveDiffPaneVariant(isEmpty: boolean, isChanged: boolean): DiffPaneVariant {
    if (isEmpty) {
        return 'placeholder';
    }
    return isChanged ? 'changed' : 'unchanged';
}

/** Left pane = removals; right pane = additions; gap tints mirror the opposite side. */
export function resolveDiffPaneTone(side: DiffPaneSide, variant: DiffPaneVariant): DiffPaneTone {
    if (variant === 'unchanged') {
        return 'neutral';
    }
    if (variant === 'placeholder') {
        return side === 'left' ? 'added-gap' : 'removed-gap';
    }
    return side === 'left' ? 'removed' : 'added';
}

export function diffPaneToneClasses(tone: DiffPaneTone) {
    return DEPLOYMENT_DIFF.tone[tone];
}
