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

import { GroupRoleSelect } from './GroupRoleSelect';
import type { GroupRole } from '../types/group';
import type { MemberRoleLockFlags, MemberRoleSelections, RoleField } from '../utils/memberRoles';

export function GroupScopedRoleSelects({
    idPrefix = 'group-role',
    roles,
    values,
    onChange,
    locks,
    disabled = false,
    disabledOptionNames,
}: Readonly<{
    idPrefix?: string;
    roles: {
        api: GroupRole[];
        apiProduct: GroupRole[];
        application: GroupRole[];
        integration: GroupRole[];
        cluster: GroupRole[];
        explorer: GroupRole[];
    };
    values: MemberRoleSelections;
    onChange: (field: RoleField, value: string) => void;
    locks: MemberRoleLockFlags;
    disabled?: boolean;
    disabledOptionNames?: {
        api?: Set<string>;
        apiProduct?: Set<string>;
    };
}>) {
    return (
        <div className="grid grid-cols-2 gap-4">
            <GroupRoleSelect
                id={`${idPrefix}-api`}
                label="API"
                roles={roles.api}
                value={values.apiRole}
                onChange={value => onChange('apiRole', value)}
                disabled={disabled || locks.api}
                disabledOptionNames={disabledOptionNames?.api}
                disableSystemRoles="except-primary-owner"
            />
            <GroupRoleSelect
                id={`${idPrefix}-api-product`}
                label="API product"
                roles={roles.apiProduct}
                value={values.apiProductRole}
                onChange={value => onChange('apiProductRole', value)}
                disabled={disabled || locks.apiProduct}
                disabledOptionNames={disabledOptionNames?.apiProduct}
                disableSystemRoles="except-primary-owner"
            />
            <GroupRoleSelect
                id={`${idPrefix}-application`}
                label="Application"
                roles={roles.application}
                value={values.applicationRole}
                onChange={value => onChange('applicationRole', value)}
                disabled={disabled || locks.application}
                disableSystemRoles="all"
            />
            <GroupRoleSelect
                id={`${idPrefix}-integration`}
                label="Integration"
                roles={roles.integration}
                value={values.integrationRole}
                onChange={value => onChange('integrationRole', value)}
                disabled={disabled || locks.integration}
                disableSystemRoles="all"
            />
            <GroupRoleSelect
                id={`${idPrefix}-cluster`}
                label="Cluster"
                roles={roles.cluster}
                value={values.clusterRole}
                onChange={value => onChange('clusterRole', value)}
                disabled={disabled || locks.cluster}
                disableSystemRoles="all"
            />
            <GroupRoleSelect
                id={`${idPrefix}-explorer`}
                label="Explorer"
                roles={roles.explorer}
                value={values.explorerRole}
                onChange={value => onChange('explorerRole', value)}
                disabled={disabled || locks.explorer}
                disableSystemRoles="all"
            />
        </div>
    );
}
