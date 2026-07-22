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
import { ExternalLinkIcon, Trash2Icon, Wand2Icon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { buildStandalonePortalUrl, usePortalApp } from '../../../app/PortalAppContext';
import { DeletePortalDialog } from '../../portals/components/DeletePortalDialog';
import { usePortalsNavigation } from '../../portals/config/navigation';
import { deletePortalWithRelatedData } from '../../portals/storage/portals.storage';
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
    const navigate = useNavigate();
    const { homePath } = usePortalsNavigation();
    const { embeddedInConsole, standaloneEditorBaseUrl } = usePortalApp();
    const { portal, loading, missing, updateSettings } = usePortal(portalId);

    const [name, setName] = useState('');
    const [portalUrl, setPortalUrl] = useState('');
    const [documentationViewer, setDocumentationViewer] =
        useState<PortalDocumentationViewer>(DEFAULT_DOCUMENTATION_VIEWER);
    const [deleteOpen, setDeleteOpen] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);

    useEffect(() => {
        if (!portal) {
            return;
        }
        setName(portal.name);
        setPortalUrl(portal.portalUrl ?? '');
        setDocumentationViewer(portal.documentationViewer ?? DEFAULT_DOCUMENTATION_VIEWER);
    }, [portal]);

    const openEditorPath = useCallback(
        (path: string) => {
            if (embeddedInConsole) {
                window.open(buildStandalonePortalUrl(standaloneEditorBaseUrl, path), '_blank', 'noopener,noreferrer');
                return;
            }
            navigate(path);
        },
        [embeddedInConsole, navigate, standaloneEditorBaseUrl],
    );

    const handleOpenPortal = useCallback(() => {
        if (!portal) {
            return;
        }
        const publicPortalUrl = portal.portalUrl?.trim();
        if (publicPortalUrl) {
            window.open(publicPortalUrl, '_blank', 'noopener,noreferrer');
            return;
        }
        openEditorPath(`/portals/${portal.id}`);
    }, [openEditorPath, portal]);

    const handleOpenDesigner = useCallback(() => {
        if (!portal) {
            return;
        }
        openEditorPath(`/portals/${portal.id}/edit`);
    }, [openEditorPath, portal]);

    const handleConfirmDelete = useCallback(async () => {
        if (!portal) {
            return;
        }
        setIsDeleting(true);
        try {
            await deletePortalWithRelatedData(portal.id);
            setDeleteOpen(false);
            notify.success('Portal deleted.');
            navigate(homePath);
        } finally {
            setIsDeleting(false);
        }
    }, [homePath, navigate, portal]);

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
    const trimmedName = name.trim();
    const canSave = trimmedName.length >= 2;
    const isDirty =
        trimmedName !== portal.name || portalUrl !== savedUrl || documentationViewer !== savedViewer;

    const handleSave = async () => {
        if (!canSave) {
            return;
        }
        await updateSettings({
            name: trimmedName,
            portalUrl: portalUrl.trim(),
            documentationViewer,
        });
        notify.success('Portal settings saved.');
    };

    const handleCancel = () => {
        setName(portal.name);
        setPortalUrl(portal.portalUrl ?? '');
        setDocumentationViewer(portal.documentationViewer ?? DEFAULT_DOCUMENTATION_VIEWER);
    };

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                    <p className="text-sm text-muted-foreground">{meta.description}</p>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                    <Button type="button" variant="outline" className="gap-2" onClick={handleOpenPortal}>
                        <ExternalLinkIcon className="size-4" aria-hidden />
                        Open portal
                    </Button>
                    <Button type="button" variant="outline" className="gap-2" onClick={handleOpenDesigner}>
                        <Wand2Icon className="size-4" aria-hidden />
                        Portal Designer
                    </Button>
                    <Button type="button" variant="outline" onClick={handleCancel} disabled={!isDirty}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={() => void handleSave()} disabled={!canSave || !isDirty}>
                        Save Changes
                    </Button>
                </div>
            </div>

            <Card>
                <CardContent className="space-y-4 p-5">
                    <Field>
                        <FieldLabel htmlFor="portal-name">
                            Name <span className="text-destructive">*</span>
                        </FieldLabel>
                        <Input
                            id="portal-name"
                            value={name}
                            onChange={event => setName(event.target.value)}
                            placeholder="Payments API Portal"
                            required
                        />
                    </Field>

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

            <section aria-labelledby="portal-danger-zone-heading" className="space-y-3">
                <div className="space-y-1">
                    <h2 id="portal-danger-zone-heading" className="text-base font-semibold tracking-tight">
                        Danger Zone
                    </h2>
                    <p className="text-sm text-muted-foreground">
                        Permanently delete this portal and all of its pages. This action cannot be undone.
                    </p>
                </div>
                <Card className="border-destructive/40">
                    <CardContent className="flex flex-wrap items-center justify-between gap-4 p-5">
                        <div className="space-y-1">
                            <p className="text-sm font-medium">Delete this portal</p>
                            <p className="text-xs text-muted-foreground">
                                Removes the portal, its pages, and related configuration.
                            </p>
                        </div>
                        <Button
                            type="button"
                            variant="destructive"
                            className="gap-2"
                            onClick={() => setDeleteOpen(true)}
                        >
                            <Trash2Icon className="size-4" aria-hidden />
                            Delete portal
                        </Button>
                    </CardContent>
                </Card>
            </section>

            <DeletePortalDialog
                portal={portal}
                open={deleteOpen}
                isPending={isDeleting}
                onOpenChange={setDeleteOpen}
                onConfirm={() => void handleConfirmDelete()}
            />
        </div>
    );
}
