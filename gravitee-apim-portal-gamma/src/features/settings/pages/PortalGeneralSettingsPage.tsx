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
import { Button, Card, CardContent, Field, FieldLabel, Input } from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { usePortalsNavigation } from '../../portals/config/navigation';
import {
    DEFAULT_DOCUMENTATION_VIEWER,
    PORTAL_DOCUMENTATION_VIEWER_LABELS,
    PORTAL_DOCUMENTATION_VIEWERS,
    type PortalDocumentationViewer,
} from '../../portals/types';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { notify } from '../../../shared/notify/notify';
import { usePortal } from '../hooks/usePortal';
import { PORTAL_SETTINGS_SECTION_META } from '../types';

export function PortalGeneralSettingsPage() {
    const { portalId = '' } = useParams<{ portalId: string }>();
    const { homePath, portalSettingsPath } = usePortalsNavigation();
    const { portal, loading, missing, updateSettings } = usePortal(portalId);

    const [portalUrl, setPortalUrl] = useState('');
    const [documentationViewer, setDocumentationViewer] =
        useState<PortalDocumentationViewer>(DEFAULT_DOCUMENTATION_VIEWER);

    useEffect(() => {
        if (!portal) {
            return;
        }
        setPortalUrl(portal.portalUrl ?? '');
        setDocumentationViewer(portal.documentationViewer ?? DEFAULT_DOCUMENTATION_VIEWER);
    }, [portal]);

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

    const meta = PORTAL_SETTINGS_SECTION_META.settings;
    const savedUrl = portal.portalUrl ?? '';
    const savedViewer = portal.documentationViewer ?? DEFAULT_DOCUMENTATION_VIEWER;
    const isDirty = portalUrl !== savedUrl || documentationViewer !== savedViewer;

    const handleSave = async () => {
        await updateSettings({
            portalUrl: portalUrl.trim(),
            documentationViewer,
        });
        notify.success('Portal settings saved.');
    };

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="space-y-1">
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{portal.name}</p>
                <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                <p className="text-sm text-muted-foreground">{meta.description}</p>
            </div>

            <Card>
                <CardContent className="space-y-4 p-5">
                    <Field>
                        <FieldLabel htmlFor="portal-url">Portal URL</FieldLabel>
                        <Input
                            id="portal-url"
                            type="url"
                            value={portalUrl}
                            onChange={event => setPortalUrl(event.target.value)}
                            placeholder="https://portal.example.com"
                        />
                    </Field>

                    <Field>
                        <FieldLabel htmlFor="portal-documentation-viewer">Documentation viewer</FieldLabel>
                        <select
                            id="portal-documentation-viewer"
                            value={documentationViewer}
                            onChange={event =>
                                setDocumentationViewer(event.target.value as PortalDocumentationViewer)
                            }
                            className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                        >
                            {PORTAL_DOCUMENTATION_VIEWERS.map(viewer => (
                                <option key={viewer} value={viewer}>
                                    {PORTAL_DOCUMENTATION_VIEWER_LABELS[viewer]}
                                </option>
                            ))}
                        </select>
                    </Field>
                </CardContent>
            </Card>

            <div className="flex flex-wrap items-center gap-2">
                <Button type="button" onClick={() => void handleSave()} disabled={!isDirty}>
                    Save
                </Button>
                <Button variant="outline" asChild>
                    <Link to={portalSettingsPath(portal.id)}>Back to {portal.name}</Link>
                </Button>
            </div>
        </div>
    );
}
