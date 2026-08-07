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

import { Button } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useNavigate, useParams } from 'react-router-dom';

/**
 * Placeholder for FOUND-34 / FOUND-35 detail + monitoring tabs.
 * Keeps list → detail navigation from crashing until those stories land.
 */
export function GatewayInstanceDetailStubPage() {
    const { instanceId } = useParams<{ instanceId: string }>();
    const navigate = useNavigate();

    return (
        <div className="space-y-6">
            <Button type="button" variant="ghost" className="gap-2 px-0" onClick={() => navigate('../..')}>
                <ArrowLeftIcon className="size-4" aria-hidden />
                Back to Gateways
            </Button>
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Gateway instance</h1>
                <p className="text-sm text-muted-foreground font-mono">{instanceId}</p>
                <p className="text-sm text-muted-foreground pt-2">
                    Detail (Environment) and Monitoring tabs will be implemented in FOUND-34 and FOUND-35.
                </p>
            </div>
        </div>
    );
}
