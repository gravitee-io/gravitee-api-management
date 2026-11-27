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
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { portalKeys } from '../utils/queryKeys';

export function PortalOverviewPlaceholderPage() {
    const { portalId } = useParams<{ portalId: string }>();
    const environmentId = useEnvironment()?.id ?? '';
    const { data: refreshStatus } = useQuery({
        queryKey: portalKeys.creationRefresh(environmentId, portalId ?? ''),
        queryFn: async () => 'success',
        enabled: false,
    });
    return (
        <div>
            <h1>Portal overview</h1>
            {refreshStatus === 'error' ? <p role="alert">Portal created, but the portal list or quota could not be refreshed.</p> : null}
            <p>{portalId}</p>
        </div>
    );
}
