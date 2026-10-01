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
import { KeyIcon } from '@gravitee/graphene-core/icons';

interface CloudTokensEmptyStateProps {
    readonly testId?: string;
}

export function CloudTokensEmptyState({ testId = 'cloud-tokens-empty' }: CloudTokensEmptyStateProps) {
    return (
        <div className="flex flex-col items-center justify-center gap-3 py-16 text-center" data-testid={testId}>
            <div className="flex size-14 items-center justify-center rounded-full bg-primary/10 text-primary">
                <KeyIcon className="size-7" aria-hidden />
            </div>
            <div className="space-y-1">
                <p className="text-base font-semibold">No tokens... yet</p>
                <p className="text-sm text-muted-foreground">There are no tokens generated for this account.</p>
            </div>
        </div>
    );
}
