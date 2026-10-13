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
import { Button, Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, Spinner } from '@gravitee/graphene-core';

import { MessageTitle } from '../../../shared/components/MessageTitle';
import { useMinimumDuration } from '../../../shared/hooks/useMinimumDuration';

/** An attempt that fails can end within a few milliseconds: "Retrying…" stays long enough to be read. */
const RETRYING_SHOWN_FOR_AT_LEAST_MS = 600;

interface ModuleUpdatingProps {
    readonly moduleName: string;
    readonly attempting: boolean;
    readonly onRetryNow: () => void;
}

export function ModuleUpdating({ moduleName, attempting, onRetryNow }: ModuleUpdatingProps) {
    const retrying = useMinimumDuration(attempting, RETRYING_SHOWN_FOR_AT_LEAST_MS);

    return (
        <Empty>
            <EmptyHeader>
                <EmptyMedia variant="icon">
                    <Spinner aria-hidden="true" />
                </EmptyMedia>
                <MessageTitle>{`${moduleName} isn't ready yet`}</MessageTitle>
                <EmptyDescription>
                    {"An update may be in progress. We will keep retrying and open it here as soon as it's ready."}
                </EmptyDescription>
            </EmptyHeader>
            <EmptyContent>
                {/* Outline on purpose: the console keeps retrying by itself, so waiting is the expected path and this
                    button only saves time. The primary style is kept for the final message, where the user has to act.
                    The minimum width keeps its size when the label changes. */}
                <Button variant="outline" className="min-w-24" aria-disabled={retrying} onClick={retrying ? undefined : onRetryNow}>
                    {retrying ? 'Retrying…' : 'Retry now'}
                </Button>
            </EmptyContent>
        </Empty>
    );
}
