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
import { Card, CardContent, CardDescription, CardHeader, CardTitle, cn } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

interface AuthPageShellProps {
    readonly title: string;
    readonly description: string;
    readonly children: ReactNode;
    /** Trailing navigation slot, rendered under the content in muted body text. */
    readonly footer?: ReactNode;
}

/**
 * Layout shared by every anonymous page: sign-in, password reset, and the
 * registration pages that follow. Owns the centring, the card width and the
 * header rhythm so the pages stay one visual family.
 */
export function AuthPageShell({ title, description, children, footer }: AuthPageShellProps) {
    return (
        <div className={cn('flex min-h-screen flex-col items-center justify-center p-4', 'font-sans text-foreground')}>
            <Card className="w-full max-w-md shadow-md">
                <CardHeader className="space-y-1 text-center">
                    <CardTitle className="text-2xl">{title}</CardTitle>
                    <CardDescription>{description}</CardDescription>
                </CardHeader>
                <CardContent>
                    {children}
                    {footer ? <p className="mt-4 text-center text-sm text-muted-foreground">{footer}</p> : null}
                </CardContent>
            </Card>
        </div>
    );
}
