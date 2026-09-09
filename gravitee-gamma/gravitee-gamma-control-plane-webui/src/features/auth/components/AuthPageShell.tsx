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
import { Card, CardContent, CardDescription, CardHeader, CardTitle, Logo, cn } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

interface AuthPageShellProps {
    readonly title: string;
    /** Omitted where the form speaks for itself — a line that restates the fields is noise. */
    readonly description?: string;
    readonly children: ReactNode;
    /**
     * Trailing navigation slot, rendered under the content in muted body text. Wrapped in a
     * `div`, not a `p`: `ReactNode` admits block elements, which a `p` would nest invalidly.
     */
    readonly footer?: ReactNode;
}

/**
 * Layout shared by every anonymous page: sign-in, password reset, and the
 * registration pages that follow. Owns the card width and the header rhythm so
 * the pages stay one visual family.
 *
 * Everything sits on one left edge. Graphene's card header is left-aligned; the pages
 * overrode it with `text-center`, which put the header on a different axis from the
 * fields under it. Dropping the override is the design system's own default.
 *
 * No shadow: Graphene's DESIGN.md §4 reserves shadows for floating layers — the
 * No-Shadow-At-Rest Rule — and names `ring-1 ring-foreground/10`, which `Card` already
 * carries, as the containment an in-flow surface uses instead.
 *
 * The mark comes from Graphene, the same source `AppSidebar` renders, so the signed-out
 * pages cannot drift from the signed-in chrome.
 */
export function AuthPageShell({ title, description, children, footer }: AuthPageShellProps) {
    return (
        <div className={cn('flex min-h-screen flex-col items-center justify-center p-4', 'font-sans text-foreground')}>
            <Card className="w-full max-w-md">
                <CardHeader className="space-y-2">
                    <Logo size="lg" className="mb-4" />
                    <CardTitle className="text-2xl">{title}</CardTitle>
                    {description ? <CardDescription>{description}</CardDescription> : null}
                </CardHeader>
                <CardContent>
                    {children}
                    {footer ? (
                        <div data-slot="auth-page-footer" className="mt-6 text-sm text-muted-foreground">
                            {footer}
                        </div>
                    ) : null}
                </CardContent>
            </Card>
        </div>
    );
}
