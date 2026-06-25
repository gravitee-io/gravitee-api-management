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
import { Skeleton } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

import type { DeveloperPortal } from '../types';
import { CreatePortalTile } from './CreatePortalTile';
import { PortalTile } from './PortalTile';

/** 300×180 design ratio — applied once per grid cell, not per tile component. */
function PortalGridCell({ children }: { readonly children: ReactNode }) {
    return (
        <div className="h-full w-full" style={{ aspectRatio: '5 / 3' }}>
            {children}
        </div>
    );
}

function LoadingTiles() {
    return (
        <>
            {Array.from({ length: 6 }, (_, index) => (
                <PortalGridCell key={index}>
                    <Skeleton className="size-full rounded-xl" />
                </PortalGridCell>
            ))}
        </>
    );
}

export function PortalsGrid({
    portals,
    loading,
}: {
    readonly portals: readonly DeveloperPortal[];
    readonly loading: boolean;
}) {
    if (loading) {
        return (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-6" aria-busy="true">
                <LoadingTiles />
            </div>
        );
    }

    return (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-6">
            {portals.map(portal => (
                <PortalGridCell key={portal.id}>
                    <PortalTile portal={portal} />
                </PortalGridCell>
            ))}
            <PortalGridCell>
                <CreatePortalTile />
            </PortalGridCell>
        </div>
    );
}
