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
import { ArrowRightIcon } from '@gravitee/graphene-core/icons';
import { Link, useParams } from 'react-router-dom';

import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { usePortalsNavigation } from '../../portals/config/navigation';
import { usePortal } from '../hooks/usePortal';
import { PORTAL_SETTINGS_SECTION_META, PORTAL_WORKFLOWS } from '../types';

export function WorkflowsPage() {
    const { portalId = '' } = useParams<{ portalId: string }>();
    const { homePath, portalSettingsPath, portalSettingsSectionPath } = usePortalsNavigation();
    const { portal, loading, missing } = usePortal(portalId);

    if (loading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading workflows…</p>;
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

    const meta = PORTAL_SETTINGS_SECTION_META.workflows;
    const workflowsBasePath = portalSettingsSectionPath(portal.id, 'workflows');

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="space-y-1">
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{portal.name}</p>
                <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                <p className="text-sm text-muted-foreground">{meta.description}</p>
            </div>

            <section aria-labelledby="portal-workflows-heading" className="space-y-3">
                <h2 id="portal-workflows-heading" className="text-base font-semibold tracking-tight">
                    Workflows
                </h2>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {PORTAL_WORKFLOWS.map(workflow => (
                        <Link
                            key={workflow.id}
                            to={`${workflowsBasePath}/${workflow.id}`}
                            className="block h-full rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                        >
                            <Card className="h-full min-h-44 transition-shadow duration-150 hover:shadow-md">
                                <CardContent className="flex h-full min-h-44 flex-col gap-3 p-5">
                                    <h3 className="line-clamp-2 text-sm font-semibold leading-tight">
                                        {workflow.title}
                                    </h3>
                                    <p className="line-clamp-3 flex-1 text-xs text-muted-foreground">
                                        {workflow.description}
                                    </p>
                                    <p className="mt-auto flex items-center gap-1 text-xs font-medium text-muted-foreground">
                                        Configure
                                        <ArrowRightIcon className="size-3" aria-hidden />
                                    </p>
                                </CardContent>
                            </Card>
                        </Link>
                    ))}
                </div>
            </section>

            <Button variant="outline" asChild>
                <Link to={portalSettingsPath(portal.id)}>Back to {portal.name}</Link>
            </Button>
        </div>
    );
}
