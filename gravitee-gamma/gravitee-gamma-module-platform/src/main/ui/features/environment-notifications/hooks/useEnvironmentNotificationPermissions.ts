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

/**
 * `canUpdatePortal` is intentionally derived from `-r`, not `-u`: Classic's
 * `NotificationConfigsResource#updatePortalNotificationSettings` only requires
 * `ENVIRONMENT_NOTIFICATION` READ, since the Console/Portal row is the caller's own
 * notification preference, not a shared GENERIC config. `canUpdateGeneric` requires `-u`,
 * matching `updateGenericNotificationSettings`.
 */
export function useEnvironmentNotificationPermissions() {
    const canCreate = useHasPermission({ anyOf: ['environment-notification-c'] });
    const canUpdateGeneric = useHasPermission({ anyOf: ['environment-notification-u'] });
    const canUpdatePortal = useHasPermission({ anyOf: ['environment-notification-r'] });
    const canDelete = useHasPermission({ anyOf: ['environment-notification-d'] });

    return { canCreate, canUpdateGeneric, canUpdatePortal, canDelete };
}
