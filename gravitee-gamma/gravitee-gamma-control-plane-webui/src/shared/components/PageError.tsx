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
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia } from '@gravitee/graphene-core';
import { CircleAlert } from 'lucide-react';
import type { ReactNode } from 'react';

import { MessageTitle } from './MessageTitle';

/** The description of a page error that **Retry** may fix: the platform may be updating. */
export const RETRY_LATER_DESCRIPTION =
    'The platform may be updating. Try again in a minute. If this keeps happening, contact your administrator.';

interface PageErrorProps {
    readonly title: string;
    readonly description: string;
    /** The way forward, a button. */
    readonly children: ReactNode;
}

/** Shown in place of a page that cannot be shown, with the way forward. It never shows the technical error. */
export function PageError({ title, description, children }: PageErrorProps) {
    return (
        <Empty role="alert">
            <EmptyHeader>
                {/* Red, which Graphene keeps for errors: a message shown while the console still retries stays neutral. */}
                <EmptyMedia variant="icon" className="bg-destructive/10 text-destructive">
                    <CircleAlert aria-hidden="true" />
                </EmptyMedia>
                <MessageTitle>{title}</MessageTitle>
                <EmptyDescription>{description}</EmptyDescription>
            </EmptyHeader>
            <EmptyContent>{children}</EmptyContent>
        </Empty>
    );
}
