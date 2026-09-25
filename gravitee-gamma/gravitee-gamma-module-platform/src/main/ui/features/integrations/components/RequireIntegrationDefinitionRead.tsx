/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { type ReactElement, useEffect } from 'react';
import { Navigate, useParams } from 'react-router-dom';

import { useIntegrationPermissions } from '../hooks/useIntegrationPermissions';

const INTEGRATION_DEFINITION_READ_PERMISSION = 'integration-definition-r';

export function RequireIntegrationDefinitionRead({ children }: Readonly<{ children: ReactElement }>) {
    const { integrationId = '' } = useParams<{ integrationId: string }>();
    const { data: permissions, isError, error } = useIntegrationPermissions(integrationId);

    useEffect(() => {
        if (isError) {
            console.warn(`Integration permissions for ${integrationId} could not be loaded; redirecting to the Integrations list`, error);
        }
    }, [isError, error, integrationId]);

    if (isError) return <Navigate to=".." replace />;
    if (!permissions) return null;
    if (!permissions.includes(INTEGRATION_DEFINITION_READ_PERMISSION)) return <Navigate to=".." replace />;
    return children;
}
