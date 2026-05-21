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

import { useApplicationDetailContext } from '../context/ApplicationDetailContext';

/**
 * Application subscription permissions (console: application-subscription-*).
 * Waits for {@link useApplicationPermissions} before exposing flags to avoid action flicker.
 */
export function useApplicationSubscriptionPermissions() {
    const { permissionsReady } = useApplicationDetailContext();

    const canRead = useHasPermission({ anyOf: ['application-subscription-r'] });
    const canCreate = useHasPermission({ anyOf: ['application-subscription-c'] });
    const canUpdate = useHasPermission({ anyOf: ['application-subscription-u'] });
    const canDelete = useHasPermission({ anyOf: ['application-subscription-d'] });
    /** Matches subscriptions tab/outlet (`application-subscription-r`) and subscription GET on the API. */
    const canViewDetail = canRead;

    const ready = permissionsReady;

    return {
        permissionsReady: ready,
        canRead: ready && canRead,
        canCreate: ready && canCreate,
        canUpdate: ready && canUpdate,
        canDelete: ready && canDelete,
        canViewDetail: ready && canViewDetail,
    };
}
