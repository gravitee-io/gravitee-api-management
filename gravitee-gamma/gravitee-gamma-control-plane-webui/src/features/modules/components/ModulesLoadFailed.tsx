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
import { PageError, RETRY_LATER_DESCRIPTION } from '../../../shared/components/PageError';
import { RetryButton } from '../../../shared/components/RetryButton';

interface ModulesLoadFailedProps {
    /** A retry is running. */
    readonly retrying: boolean;
    readonly onRetry: () => void;
}

/**
 * Shown in place of a page when the list of modules could not be loaded. The address may be a module's:
 * it is kept, and the page opens once the list is there.
 */
export function ModulesLoadFailed({ retrying, onRetry }: ModulesLoadFailedProps) {
    return (
        <PageError title="Couldn't load your applications" description={RETRY_LATER_DESCRIPTION}>
            <RetryButton retrying={retrying} onRetry={onRetry} />
        </PageError>
    );
}
