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
import { useEffect, useState } from 'react';

import { useBootstrapStore } from '../../../shared/config/bootstrap.store';

/**
 * The Access Management tile. The `am` module asks the SSO issuer for a one-time token and returns the AM
 * console URL that consumes it; the browser then leaves for the AM console in the current tab.
 */
export function AccessManagementRedirect() {
    const gammaBaseURL = useBootstrapStore(s => s.config?.gammaBaseURL);
    const organizationId = useBootstrapStore(s => s.config?.organizationId);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!gammaBaseURL || !organizationId) {
            return;
        }
        const controller = new AbortController();
        fetch(`${gammaBaseURL}/organizations/${organizationId}/modules/am/sso`, { credentials: 'include', signal: controller.signal })
            .then(res => {
                if (!res.ok) throw new Error(`Access Management sign-in failed: ${res.status}`);
                return res.json() as Promise<{ url: string }>;
            })
            .then(({ url }) => window.location.assign(url))
            .catch((err: unknown) => {
                if (!controller.signal.aborted) {
                    setError(err instanceof Error ? err.message : String(err));
                }
            });
        return () => controller.abort();
    }, [gammaBaseURL, organizationId]);

    return error ? <p className="p-6 text-sm text-destructive">{error}</p> : null;
}
