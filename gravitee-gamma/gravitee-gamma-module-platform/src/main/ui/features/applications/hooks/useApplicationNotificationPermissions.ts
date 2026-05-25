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

export function useApplicationNotificationPermissions() {
    const { permissionsReady } = useApplicationDetailContext();

    const canCreateNotification = useHasPermission({ anyOf: ['application-notification-c'] });
    const canUpdateNotification = useHasPermission({ anyOf: ['application-notification-u'] });
    const canCreateMetadata = useHasPermission({ anyOf: ['application-metadata-c'] });
    const canUpdateMetadata = useHasPermission({ anyOf: ['application-metadata-u'] });
    const canDeleteMetadata = useHasPermission({ anyOf: ['application-metadata-d'] });

    return {
        permissionsReady,
        canCreateNotification: permissionsReady && canCreateNotification,
        canUpdateNotification: permissionsReady && canUpdateNotification,
        canCreateMetadata: permissionsReady && canCreateMetadata,
        canUpdateMetadata: permissionsReady && canUpdateMetadata,
        canDeleteMetadata: permissionsReady && canDeleteMetadata,
    };
}
