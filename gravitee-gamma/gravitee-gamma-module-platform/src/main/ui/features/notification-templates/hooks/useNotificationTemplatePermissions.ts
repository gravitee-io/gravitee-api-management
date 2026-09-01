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

import {
    ORGANIZATION_NOTIFICATION_TEMPLATES_CREATE,
    ORGANIZATION_NOTIFICATION_TEMPLATES_READ,
    ORGANIZATION_NOTIFICATION_TEMPLATES_UPDATE,
} from '../utils/permissions';

export function useNotificationTemplatePermissions() {
    const canRead = useHasPermission({ anyOf: [ORGANIZATION_NOTIFICATION_TEMPLATES_READ] });
    const canCreate = useHasPermission({ anyOf: [ORGANIZATION_NOTIFICATION_TEMPLATES_CREATE] });
    const canUpdate = useHasPermission({ anyOf: [ORGANIZATION_NOTIFICATION_TEMPLATES_UPDATE] });
    const canEdit = canCreate || canUpdate;

    return { canRead, canCreate, canUpdate, canEdit };
}
