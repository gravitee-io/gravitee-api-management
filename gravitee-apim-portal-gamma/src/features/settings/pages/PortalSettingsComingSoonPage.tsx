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
import { Link, useParams } from 'react-router-dom';

import { usePortalsNavigation } from '../../portals/config/navigation';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { usePortal } from '../hooks/usePortal';
import { PORTAL_SETTINGS_SECTION_META, type PortalSettingsSection } from '../types';

const SECTION_BY_PATH: Record<string, PortalSettingsSection> = {
    categories: 'categories',
    'subscription-forms': 'subscription-form',
    workflows: 'workflows',
    idp: 'idp-configuration',
};

export function PortalSettingsComingSoonPage() {
    const { portalId = '', section = '' } = useParams<{ portalId: string; section?: string }>();
    const { homePath, portalSettingsPath } = usePortalsNavigation();
    const { portal, loading, missing } = usePortal(portalId);

    if (loading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading portal settings…</p>;
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

    const resolvedSection = SECTION_BY_PATH[section];
    if (!resolvedSection) {
        return (
            <NotFoundPage
                homePath={portalSettingsPath(portal.id)}
                homeLabel={`Back to ${portal.name}`}
                title="Settings section not found"
                description="This portal settings section does not exist or has been moved."
            />
        );
    }

    const meta = PORTAL_SETTINGS_SECTION_META[resolvedSection];

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="space-y-1">
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{portal.name}</p>
                <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                <p className="text-sm text-muted-foreground">{meta.description}</p>
            </div>

            <Card>
                <CardContent className="p-5 text-sm text-muted-foreground">
                    Configuration UI for <span className="font-medium text-foreground">{meta.title}</span> is
                    coming next.
                </CardContent>
            </Card>

            <Button variant="outline" asChild>
                <Link to={portalSettingsPath(portal.id)}>Back to {portal.name}</Link>
            </Button>
        </div>
    );
}
