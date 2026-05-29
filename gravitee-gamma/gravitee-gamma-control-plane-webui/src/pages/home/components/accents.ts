/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Card accents — maps to Graphene semantic tokens. */
export type Accent = 'primary' | 'success' | 'highlight' | 'accent' | 'muted';

/**
 * `hoverColor` is a CSS variable reference applied via inline style on mouseenter/leave.
 * Graphene's pre-built CSS only ships hover:border-primary, so we drive the other
 * accent border colors through JS rather than adding a separate CSS file.
 */
export const ACCENT_CLASSES: Record<Accent, { readonly bg: string; readonly fg: string; readonly hoverColor: string }> = {
    primary: { bg: 'bg-primary/10', fg: 'text-primary', hoverColor: 'var(--color-primary)' },
    success: { bg: 'bg-success/10', fg: 'text-success', hoverColor: 'var(--color-success)' },
    highlight: { bg: 'bg-highlight/10', fg: 'text-highlight', hoverColor: 'var(--color-highlight)' },
    accent: { bg: 'bg-accent', fg: 'text-accent-foreground', hoverColor: 'var(--color-foreground)' },
    muted: { bg: 'bg-muted', fg: 'text-muted-foreground', hoverColor: 'var(--color-muted-foreground)' },
};
