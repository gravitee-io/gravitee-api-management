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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Button } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import { AlertsEducationalEmptyState } from '../features/alerts/components/AlertsEducationalEmptyState';
import { ENVIRONMENT_ALERT_CREATE_PERMISSION } from '../features/alerts/utils/alertPermissions';

/**
 * Environment-level Alerts landing page.
 *
 * Skeleton for APIM-14910 (educational UX): the alert list and activity views land in
 * follow-up tickets (APIM-14911 and beyond), so this page currently always renders the
 * educational content that introduces the feature. Create is gated here so users with
 * environment-alert-c see Add alert (same as API Alerts empty state); the form route
 * lands in a follow-up ticket.
 */
export function AlertsPage() {
    const navigate = useNavigate();
    const canCreate = useHasPermission({ anyOf: [ENVIRONMENT_ALERT_CREATE_PERMISSION] });

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Alerts</h1>
                    <p className="text-sm text-muted-foreground">Get notified when your gateways or platform need attention.</p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                    {canCreate && (
                        <Button type="button" size="sm" onClick={() => navigate('new')}>
                            <PlusIcon className="size-4" aria-hidden="true" />
                            Add alert
                        </Button>
                    )}
                </div>
            </div>

            <AlertsEducationalEmptyState />
        </div>
    );
}
