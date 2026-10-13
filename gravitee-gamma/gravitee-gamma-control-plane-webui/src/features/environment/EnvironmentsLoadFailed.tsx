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
import { useEnvironmentStore } from './environment.store';
import { PageError, RETRY_LATER_DESCRIPTION } from '../../shared/components/PageError';
import { RetryButton } from '../../shared/components/RetryButton';

/**
 * Shown in place of the console's pages when the environments could not be loaded: every page belongs
 * to an environment, so there is nothing else to show until they are there.
 */
export function EnvironmentsLoadFailed() {
    const organizationId = useEnvironmentStore(s => s.organizationId);
    const initialize = useEnvironmentStore(s => s.initialize);
    const loading = useEnvironmentStore(s => s.loading);

    return (
        <PageError title="Couldn't load your environments" description={RETRY_LATER_DESCRIPTION}>
            <RetryButton retrying={loading} onRetry={() => void initialize(organizationId)} />
        </PageError>
    );
}
