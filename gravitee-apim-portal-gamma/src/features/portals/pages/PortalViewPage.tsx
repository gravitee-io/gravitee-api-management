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
import { Button } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { getPortal } from '../storage/portals.storage';
import type { DeveloperPortal } from '../types';

function BackToDashboardsLink() {
    return (
        <Button variant="ghost" size="sm" className="-ml-2 w-fit gap-1.5" asChild>
            <Link to="/">
                <ArrowLeftIcon className="size-4" aria-hidden="true" />
                Back to dashboards
            </Link>
        </Button>
    );
}

export function PortalViewPage() {
    const { id } = useParams<{ id: string }>();
    const [portal, setPortal] = useState<DeveloperPortal | undefined>();
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!id) {
            setLoading(false);
            return;
        }

        void getPortal(id).then(result => {
            setPortal(result);
            setLoading(false);
        });
    }, [id]);

    if (loading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading portal…</p>;
    }

    if (!portal) {
        return (
            <div className="space-y-4 p-6">
                <BackToDashboardsLink />
                <p className="text-sm text-muted-foreground">Portal not found.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6 p-6">
            <BackToDashboardsLink />
            <div className="space-y-2">
                <h1 className="text-2xl font-bold tracking-tight">{portal.name}</h1>
                <p className="text-sm text-muted-foreground">View mode — coming soon.</p>
            </div>
        </div>
    );
}
