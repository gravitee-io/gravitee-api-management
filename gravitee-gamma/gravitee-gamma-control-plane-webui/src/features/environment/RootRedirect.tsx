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
import { Navigate } from 'react-router-dom';

import { useEnvironmentStore } from './environment.store';
import { getPrimaryHrid } from './environment.utils';

/**
 * Sends users to the first environment "home" when the URL has no environment segment
 * (e.g. legacy `/` or unknown paths under the protected area).
 */
export function RootRedirect() {
    const environments = useEnvironmentStore(s => s.environments);
    const loading = useEnvironmentStore(s => s.loading);
    const error = useEnvironmentStore(s => s.error);

    if (error) {
        return (
            <p role="alert" className="text-destructive p-4">
                {error.message}
            </p>
        );
    }

    if (!environments.length) {
        if (loading) return <p>Loading environments…</p>;
        return (
            <p role="alert" className="text-destructive p-4">
                No environments available.
            </p>
        );
    }

    const hrid = getPrimaryHrid(environments[0]!);
    return <Navigate to={`/environments/${hrid}/home`} replace />;
}
