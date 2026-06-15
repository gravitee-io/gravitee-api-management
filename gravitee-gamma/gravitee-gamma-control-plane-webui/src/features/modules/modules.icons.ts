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
import {
    GioAgentManagementIcon,
    GioApiManagementIcon,
    GioAuthorizationIcon,
    GioEventApiManagementIcon,
    GioHomeIcon,
    GioPlatformIcon,
} from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

/**
 * Canonical mapping from module id to its product icon.
 * Shared by the app switcher (ShellLayout) and the home page cards.
 */
export const HOME_ICON = GioHomeIcon;

export const MODULE_ICONS: Record<string, LucideIcon> = {
    aim: GioAgentManagementIcon,
    apim: GioApiManagementIcon,
    platform: GioPlatformIcon,
    authz: GioAuthorizationIcon,
    esm: GioEventApiManagementIcon,
};
