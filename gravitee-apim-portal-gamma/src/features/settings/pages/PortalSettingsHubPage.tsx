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
import {
    ArrowRightIcon,
    FileTextIcon,
    FolderOpenIcon,
    KeyIcon,
    PencilIcon,
    SettingsIcon,
    WorkflowIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';
import { useEffect, useState, type ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';

import { buildStandalonePortalUrl, usePortalApp } from '../../../app/PortalAppContext';
import { usePortalsNavigation } from '../../portals/config/navigation';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { notify } from '../../../shared/notify/notify';
import { usePortal } from '../hooks/usePortal';
import { PORTAL_SETTINGS_SECTION_META, type PortalSettingsSection } from '../types';

const MENU_ITEMS: readonly {
    readonly section: PortalSettingsSection;
    readonly Icon: LucideIcon;
}[] = [
    { section: 'designer', Icon: PencilIcon },
    { section: 'subscription-form', Icon: FileTextIcon },
    { section: 'categories', Icon: FolderOpenIcon },
    { section: 'workflows', Icon: WorkflowIcon },
    { section: 'idp-configuration', Icon: KeyIcon },
    { section: 'settings', Icon: SettingsIcon },
];

const SETTINGS_TILE_CLASS =
    'h-full rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2';

/**
 * Same navigation behavior as dashboard Edit: new tab to standalone editor when
 * embedded in console; in-app Link when standalone. No homepage deep-link.
 */
function PortalDesignerTileLink({
    portalId,
    className,
    children,
}: {
    readonly portalId: string;
    readonly className?: string;
    readonly children: ReactNode;
}) {
    const { embeddedInConsole, standaloneEditorBaseUrl } = usePortalApp();
    const editPath = `/portals/${portalId}/edit`;

    if (embeddedInConsole) {
        const href = buildStandalonePortalUrl(standaloneEditorBaseUrl, editPath);
        return (
            <button
                type="button"
                className={`${className ?? ''} w-full cursor-pointer text-left`}
                onClick={() => window.open(href, '_blank', 'noopener,noreferrer')}
            >
                {children}
            </button>
        );
    }

    return (
        <Link to={editPath} className={className}>
            {children}
        </Link>
    );
}

export function PortalSettingsHubPage() {
    const { portalId = '' } = useParams<{ portalId: string }>();
    const { homePath, portalSettingsSectionPath } = usePortalsNavigation();
    const { portal, loading, missing, updateSettings } = usePortal(portalId);

    const [editing, setEditing] = useState(false);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');

    useEffect(() => {
        if (!portal) {
            return;
        }
        setName(portal.name);
        setDescription(portal.description ?? '');
        setEditing(false);
    }, [portal?.id, portal?.name, portal?.description]);

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

    const canSave = name.trim().length >= 2;
    const isDirty = name.trim() !== portal.name || description.trim() !== (portal.description ?? '');

    const handleSave = async () => {
        if (!canSave) {
            return;
        }

        await updateSettings({
            name: name.trim(),
            description: description.trim(),
        });
        setEditing(false);
        notify.success('Portal details updated.');
    };

    const handleCancel = () => {
        setName(portal.name);
        setDescription(portal.description ?? '');
        setEditing(false);
    };

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            {editing ? (
                <div className="space-y-4 rounded-xl border border-border/60 bg-card p-5">
                    <Field>
                        <FieldLabel htmlFor="portal-detail-name">Name</FieldLabel>
                        <Input
                            id="portal-detail-name"
                            value={name}
                            onChange={event => setName(event.target.value)}
                            placeholder="Dev Portal name"
                            autoFocus
                            required
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="portal-detail-description">Description</FieldLabel>
                        <Input
                            id="portal-detail-description"
                            value={description}
                            onChange={event => setDescription(event.target.value)}
                            placeholder="Short description"
                        />
                    </Field>
                    <div className="flex flex-wrap items-center gap-2">
                        <Button type="button" onClick={() => void handleSave()} disabled={!canSave || !isDirty}>
                            Save
                        </Button>
                        <Button type="button" variant="outline" onClick={handleCancel}>
                            Cancel
                        </Button>
                    </div>
                </div>
            ) : (
                <div className="space-y-1">
                    <div className="flex items-center gap-2">
                        <h1 className="text-2xl font-bold tracking-tight">{portal.name}</h1>
                        <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            className="size-8 shrink-0 p-0"
                            aria-label="Edit portal details"
                            onClick={() => setEditing(true)}
                        >
                            <PencilIcon className="size-4" aria-hidden />
                        </Button>
                    </div>
                    {(portal.description || portal.portalUrl) && (
                        <div className="space-y-0.5">
                            {portal.description ? (
                                <p className="text-sm text-muted-foreground">{portal.description}</p>
                            ) : null}
                            {portal.portalUrl ? (
                                <p className="text-xs text-muted-foreground">{portal.portalUrl}</p>
                            ) : null}
                        </div>
                    )}
                </div>
            )}

            <section aria-labelledby="portal-settings-heading" className="space-y-3">
                <h2 id="portal-settings-heading" className="text-base font-semibold tracking-tight">
                    Settings
                </h2>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {MENU_ITEMS.map(item => {
                        const meta = PORTAL_SETTINGS_SECTION_META[item.section];
                        const tile = (
                            <Card className="h-full min-h-44 transition-shadow duration-150 hover:shadow-md">
                                <CardContent className="flex h-full min-h-44 flex-col gap-3 p-5">
                                    <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-muted">
                                        <item.Icon className="size-5" aria-hidden />
                                    </div>
                                    <h3 className="line-clamp-2 text-sm font-semibold leading-tight">{meta.title}</h3>
                                    <p className="line-clamp-3 flex-1 text-xs text-muted-foreground">{meta.description}</p>
                                    <p className="mt-auto flex items-center gap-1 text-xs font-medium text-muted-foreground">
                                        Open
                                        <ArrowRightIcon className="size-3" aria-hidden />
                                    </p>
                                </CardContent>
                            </Card>
                        );

                        if (item.section === 'designer' || meta.path == null) {
                            return (
                                <PortalDesignerTileLink key={item.section} portalId={portal.id} className={SETTINGS_TILE_CLASS}>
                                    {tile}
                                </PortalDesignerTileLink>
                            );
                        }

                        return (
                            <Link
                                key={item.section}
                                to={portalSettingsSectionPath(portal.id, meta.path)}
                                className={SETTINGS_TILE_CLASS}
                            >
                                {tile}
                            </Link>
                        );
                    })}
                </div>
            </section>

            <Button variant="outline" asChild>
                <Link to={homePath}>Back to portals</Link>
            </Button>
        </div>
    );
}
