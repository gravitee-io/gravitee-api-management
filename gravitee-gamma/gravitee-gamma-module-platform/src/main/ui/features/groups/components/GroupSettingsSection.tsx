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

import { Badge } from '@gravitee/graphene-core';
import { LockIcon, MailIcon, SearchIcon } from '@gravitee/graphene-core/icons';

import type { Group } from '../types/group';

function DefaultRoleValue({ role, locked }: Readonly<{ role?: string; locked?: boolean }>) {
    return (
        <dd className="flex items-center gap-1.5 text-sm">
            {role || '—'}
            {locked && role ? <LockIcon className="size-3.5 text-muted-foreground" aria-label="Locked" /> : null}
        </dd>
    );
}

export function GroupSettingsSection({ group }: Readonly<{ group: Group }>) {
    return (
        <section className="space-y-4 rounded-xl border bg-card p-5">
            <div>
                <h2 className="text-base font-semibold">Settings</h2>
                <p className="text-sm text-muted-foreground">Default roles, member limits, and invitation methods for this group.</p>
            </div>
            <dl className="grid grid-cols-2 gap-x-6 gap-y-4 sm:grid-cols-3">
                <div>
                    <dt className="text-xs font-medium text-muted-foreground">Default API role</dt>
                    <DefaultRoleValue role={group.roles?.API} locked={group.lock_api_role} />
                </div>
                <div>
                    <dt className="text-xs font-medium text-muted-foreground">Default API product role</dt>
                    <DefaultRoleValue role={group.roles?.API_PRODUCT} locked={group.lock_api_product_role} />
                </div>
                <div>
                    <dt className="text-xs font-medium text-muted-foreground">Default application role</dt>
                    <DefaultRoleValue role={group.roles?.APPLICATION} locked={group.lock_application_role} />
                </div>
                <div>
                    <dt className="text-xs font-medium text-muted-foreground">Max members</dt>
                    <dd className="text-sm">{typeof group.max_invitation === 'number' ? group.max_invitation : 'Unlimited'}</dd>
                </div>
                <div>
                    <dt className="text-xs font-medium text-muted-foreground">Invitation methods</dt>
                    <dd className="flex flex-wrap items-center gap-1.5 text-sm">
                        {group.system_invitation && (
                            <Badge variant="default" className="gap-1 text-xs font-normal">
                                <SearchIcon className="size-3" aria-hidden />
                                User search
                            </Badge>
                        )}
                        {group.email_invitation && (
                            <Badge variant="default" className="gap-1 text-xs font-normal">
                                <MailIcon className="size-3" aria-hidden />
                                Email invitation
                            </Badge>
                        )}
                        {!group.system_invitation && !group.email_invitation && <span className="text-muted-foreground">None</span>}
                    </dd>
                </div>
                <div>
                    <dt className="text-xs font-medium text-muted-foreground">Notify on new members</dt>
                    <dd className="text-sm">{group.disable_membership_notifications ? 'No' : 'Yes'}</dd>
                </div>
            </dl>
        </section>
    );
}
