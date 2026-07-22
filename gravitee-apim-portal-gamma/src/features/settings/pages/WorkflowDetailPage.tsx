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
import { Button, Card, CardContent } from '@gravitee/graphene-core';
import { Link, Navigate, useParams } from 'react-router-dom';

import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { usePortalsNavigation } from '../../portals/config/navigation';
import { usePortal } from '../hooks/usePortal';
import { getPortalWorkflow } from '../types';

export function WorkflowDetailPage() {
    const { portalId = '', workflowId = '' } = useParams<{ portalId: string; workflowId: string }>();
    const { homePath, portalSettingsSectionPath } = usePortalsNavigation();
    const { portal, loading, missing } = usePortal(portalId);
    const workflow = getPortalWorkflow(workflowId);

    if (loading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading workflow…</p>;
    }

    if (missing || !portal) {
        return (
            <NotFoundPage
                homePath={homePath}
                homeLabel="Back to portals"
                title="Portal not found"
                description="This developer portal does not exist or may have been removed."
            />
        );
    }

    const workflowsPath = portalSettingsSectionPath(portal.id, 'workflows');

    if (!workflow) {
        return <Navigate to={workflowsPath} replace />;
    }

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="space-y-1">
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{portal.name}</p>
                <h1 className="text-2xl font-bold tracking-tight">{workflow.title}</h1>
                <p className="text-sm text-muted-foreground">{workflow.description}</p>
            </div>

            <Card>
                <CardContent className="space-y-2 p-5 text-sm text-muted-foreground">
                    <p>
                        Configuration UI for{' '}
                        <span className="font-medium text-foreground">{workflow.title}</span> is coming
                        soon.
                    </p>
                    <p>
                        Workflow admin is a placeholder in this POC — runtime still uses plan validation
                        (AUTO / MANUAL) for subscriptions. Detailed configuration will land in a later
                        iteration.
                    </p>
                </CardContent>
            </Card>

            <div className="flex flex-wrap items-center gap-2">
                <Button variant="outline" asChild>
                    <Link to={workflowsPath}>Back to Workflows</Link>
                </Button>
            </div>
        </div>
    );
}
