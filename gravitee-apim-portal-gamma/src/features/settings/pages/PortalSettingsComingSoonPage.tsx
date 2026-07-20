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
import { useParams } from 'react-router-dom';

import { usePortalsNavigation } from '../../portals/config/navigation';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { usePortal } from '../hooks/usePortal';

/**
 * Catch-all for unknown portal settings section paths.
 * Known sections are registered as dedicated routes ahead of this page.
 */
export function PortalSettingsComingSoonPage() {
    const { portalId = '' } = useParams<{ portalId: string }>();
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

    return (
        <NotFoundPage
            homePath={portalSettingsPath(portal.id)}
            homeLabel={`Back to ${portal.name}`}
            title="Settings section not found"
            description="This portal settings section does not exist or has been moved."
        />
    );
}
