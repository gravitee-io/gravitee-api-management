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
import {
    BoxesIcon,
    GlobeIcon,
    LayoutDashboardIcon,
    ListIcon,
    NetworkIcon,
    PlugIcon,
    SearchIcon,
    ServerIcon,
    SparklesIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';

import type { RoleScope } from '../types/role';

/** Classic `OrgSettingsRolesComponent.getScopeIcon`: ORGANIZATION → corporate_fare, ENVIRONMENT → dns. */
const ROLE_SCOPE_ICONS: Record<RoleScope, LucideIcon> = {
    ORGANIZATION: ServerIcon,
    ENVIRONMENT: GlobeIcon,
    API: LayoutDashboardIcon,
    APPLICATION: ListIcon,
    INTEGRATION: PlugIcon,
    CLUSTER: NetworkIcon,
    EXPLORER: SearchIcon,
    API_PRODUCT: BoxesIcon,
    AI_WORKSPACE: SparklesIcon,
};

export function getRoleScopeIcon(scope: RoleScope): LucideIcon {
    return ROLE_SCOPE_ICONS[scope];
}
