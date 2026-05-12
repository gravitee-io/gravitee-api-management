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
import { Card } from '@gravitee/graphene-core';
import { useLocation, useParams } from 'react-router-dom';

import { useApiDetailContext } from '../../context/ApiDetailContext';

type OverviewLocationState = { deployed?: boolean };

export function OverviewPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const location = useLocation();
    const state = location.state as OverviewLocationState | undefined;
    const deployed = state?.deployed;
    const { api } = useApiDetailContext();

    const apiName = api?.name ?? apiId ?? 'this API';

    return (
        <div className="space-y-4">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Overview</h1>
                <p className="text-sm text-muted-foreground">Traffic, policies, and next steps for {apiName}.</p>
            </div>

            {deployed !== undefined ? (
                <Card className="rounded-xl border border-primary/25 bg-primary/5 p-4">
                    <p className="text-sm font-medium text-primary">
                        {deployed
                            ? 'API created and deployed — traffic is flowing.'
                            : 'API created — not yet started. Start it from Deployment when ready.'}
                    </p>
                </Card>
            ) : null}

            <Card className="rounded-xl p-4 sm:p-6">
                <p className="text-xs text-muted-foreground">API ID</p>
                <p className="mt-1 font-mono text-sm break-all">{apiId}</p>
            </Card>

            <Card className="rounded-xl border-dashed p-8 text-center">
                <p className="text-sm text-muted-foreground">Traffic snapshot, checklist, and next steps coming soon</p>
            </Card>
        </div>
    );
}
