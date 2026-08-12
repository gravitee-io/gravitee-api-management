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

export type GroupEventName = 'API_CREATE' | 'APPLICATION_CREATE' | 'API_PRODUCT_CREATE';

export interface GroupEventRule {
    event: GroupEventName;
}

export interface Group {
    id: string;
    name: string;
    event_rules?: GroupEventRule[];
    manageable?: boolean;
    roles?: Record<string, string>;
    created_at?: number;
    updated_at?: number;
    lock_api_role?: boolean;
    lock_api_product_role?: boolean;
    lock_application_role?: boolean;
    max_invitation?: number | null;
    system_invitation?: boolean;
    email_invitation?: boolean;
    disable_membership_notifications?: boolean;
    primary_owner?: boolean;
    apiPrimaryOwner?: string;
    apiProductPrimaryOwner?: string;
}

export interface GroupsPageMeta {
    current: number;
    size: number;
    per_page: number;
    total_pages: number;
    total_elements: number;
}

export interface GroupsPagedResponse {
    data: Group[];
    metadata?: Record<string, Record<string, unknown>>;
    page: GroupsPageMeta;
}

export interface NewGroupPayload {
    name: string;
    lock_api_role: boolean;
    lock_api_product_role: boolean;
    lock_application_role: boolean;
    event_rules: GroupEventRule[];
    max_invitation: number | null;
    system_invitation: boolean;
    email_invitation: boolean;
    disable_membership_notifications: boolean;
}

export interface UpdateGroupPayload {
    name: string;
    lock_api_role: boolean;
    lock_api_product_role: boolean;
    lock_application_role: boolean;
    event_rules: GroupEventRule[];
    roles?: Record<string, string>;
    max_invitation: number | null;
    system_invitation: boolean;
    email_invitation: boolean;
    disable_membership_notifications: boolean;
}

export interface GroupRole {
    name: string;
    scope: string;
    system?: boolean;
    default?: boolean;
}

export const PRIMARY_OWNER_ROLE = 'PRIMARY_OWNER';
export const OWNER_ROLE = 'OWNER';

export interface GroupMember {
    id: string;
    displayName: string;
    roles?: Record<string, string>;
    created_at?: number;
    updated_at?: number;
}

export type GroupMembershipType = 'api' | 'application' | 'api_product';

export interface GroupMembershipItem {
    id: string;
    name: string;
    version?: string;
}

export type GroupMemberRoleScope = 'API' | 'APPLICATION' | 'API_PRODUCT' | 'INTEGRATION' | 'CLUSTER' | 'GROUP';

export interface SearchableUser {
    id?: string | null;
    reference: string;
    displayName: string;
    email?: string;
}

export interface GroupMembershipRole {
    scope: GroupMemberRoleScope;
    name: string;
}

export interface GroupMembershipPayload {
    id: string;
    reference?: string;
    roles: GroupMembershipRole[];
}

export interface GroupInvitationPayload {
    reference_type?: string;
    reference_id: string;
    email: string;
    api_role?: string;
    application_role?: string;
}

export interface GroupInvitation {
    id: string;
    reference_type?: string;
    reference_id: string;
    email: string;
    api_role?: string;
    application_role?: string;
    created_at?: number;
}

export interface OrganizationGroup {
    id: string;
    name: string;
    environmentId: string;
    environmentName: string;
}
