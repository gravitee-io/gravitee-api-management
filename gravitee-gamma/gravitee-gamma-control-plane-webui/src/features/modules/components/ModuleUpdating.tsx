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
import { Button, Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle, Spinner } from '@gravitee/graphene-core';

interface ModuleUpdatingProps {
    readonly moduleName: string;
    readonly attempting: boolean;
    readonly onRetryNow: () => void;
}

export function ModuleUpdating({ moduleName, attempting, onRetryNow }: ModuleUpdatingProps) {
    return (
        <Empty>
            <EmptyHeader>
                <EmptyMedia variant="icon">
                    <Spinner aria-hidden="true" />
                </EmptyMedia>
                {/* The page heading, in place of the one the module would have rendered. Graphene styles a bare
                    h1 as a page title: the classes are EmptyTitle's own, so the message keeps its size. */}
                <EmptyTitle>
                    <h1 className="text-sm font-medium tracking-tight">{`${moduleName} isn't ready yet`}</h1>
                </EmptyTitle>
                <EmptyDescription>
                    {"An update may be in progress. We keep retrying and will open it here as soon as it's ready."}
                </EmptyDescription>
            </EmptyHeader>
            <EmptyContent>
                {/* Outline on purpose: the console keeps retrying by itself, so waiting is the expected path and this
                    button only saves time. The primary style is kept for the final message, where the user has to act. */}
                <Button variant="outline" aria-disabled={attempting} onClick={attempting ? undefined : onRetryNow}>
                    {attempting ? 'Retrying…' : 'Retry now'}
                </Button>
            </EmptyContent>
        </Empty>
    );
}
