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
import { Badge, Card, CardContent, Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@gravitee/graphene-core';
import { CalendarIcon, ClockIcon } from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';

import { ROLE_LIST_TOOLTIP_CONTENT_CLASS, RoleListTooltipContent } from './RoleListTooltip';
import { UserAvatar } from './UserAvatar';
import type { OrganizationUser } from '../types/user';
import {
    formatCustomFieldValue,
    formatRoleNames,
    formatUserDisplayName,
    formatUserTimestamp,
    getOrganizationRoles,
} from '../utils/userDetailDisplay';
import {
    detailStatusBadgeVariant,
    formatSourceLabel,
    formatTruncatedRoleSummary,
    formatUserStatus,
    sourceBadgeVariant,
} from '../utils/userDisplay';

interface UserProfileCardProps {
    readonly user: OrganizationUser;
}

function ProfileMetaColumn({ label, icon, children }: Readonly<{ label: string; icon?: ReactNode; children: ReactNode }>) {
    return (
        <div className="space-y-1">
            <p className="text-sm text-muted-foreground">{label}</p>
            <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                {icon}
                {children}
            </div>
        </div>
    );
}

export function UserProfileCard({ user }: UserProfileCardProps) {
    const displayName = formatUserDisplayName(user);
    const organizationRoles = getOrganizationRoles(user.roles);
    const organizationRoleLabels = formatRoleNames(organizationRoles);
    const roleSummary = formatTruncatedRoleSummary(organizationRoles);
    const customFieldEntries = Object.entries(user.customFields ?? {});

    return (
        <Card>
            <CardContent className="space-y-6 pt-6">
                <div className="flex flex-col gap-5 sm:flex-row sm:items-start">
                    <UserAvatar name={displayName} size="lg" />
                    <div className="min-w-0 flex-1 space-y-1">
                        <div className="flex flex-wrap items-center gap-2">
                            <h1 className="text-2xl font-semibold tracking-tight">{displayName}</h1>
                            <Badge variant={detailStatusBadgeVariant(user.status)}>{formatUserStatus(user.status)}</Badge>
                            {user.isServiceAccount ? (
                                <Badge variant="outline" className="text-xs uppercase">
                                    Service account
                                </Badge>
                            ) : null}
                            {user.primary_owner ? (
                                <Badge variant="outline" className="text-xs uppercase">
                                    Owner
                                </Badge>
                            ) : null}
                        </div>
                        {user.email ? <p className="text-sm text-muted-foreground">{user.email}</p> : null}
                    </div>
                </div>

                <div className="grid gap-6 border-t pt-6 sm:grid-cols-2 lg:grid-cols-4">
                    <ProfileMetaColumn label="Source">
                        <Badge variant={sourceBadgeVariant(user.source)} className="font-normal">
                            {formatSourceLabel(user.source)}
                        </Badge>
                    </ProfileMetaColumn>
                    <ProfileMetaColumn label="Organization Roles">
                        {roleSummary.truncated ? (
                            <TooltipProvider delayDuration={200}>
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span className="cursor-default">{roleSummary.display}</span>
                                    </TooltipTrigger>
                                    <TooltipContent side="bottom" align="start" className={ROLE_LIST_TOOLTIP_CONTENT_CLASS}>
                                        <RoleListTooltipContent labels={organizationRoleLabels} />
                                    </TooltipContent>
                                </Tooltip>
                            </TooltipProvider>
                        ) : (
                            roleSummary.display
                        )}
                    </ProfileMetaColumn>
                    <ProfileMetaColumn label="Last Login" icon={<ClockIcon className="size-4 text-muted-foreground" aria-hidden />}>
                        {formatUserTimestamp(user.lastConnectionAt)}
                    </ProfileMetaColumn>
                    <ProfileMetaColumn label="Created" icon={<CalendarIcon className="size-4 text-muted-foreground" aria-hidden />}>
                        {formatUserTimestamp(user.created_at)}
                    </ProfileMetaColumn>
                </div>

                {customFieldEntries.length > 0 ? (
                    <dl className="grid gap-4 border-t pt-6 sm:grid-cols-2 lg:grid-cols-3">
                        {customFieldEntries.map(([key, value]) => (
                            <div key={key} className="space-y-1">
                                <dt className="text-sm font-medium">{key}</dt>
                                <dd className="text-sm text-muted-foreground [overflow-wrap:anywhere]">{formatCustomFieldValue(value)}</dd>
                            </div>
                        ))}
                    </dl>
                ) : null}
            </CardContent>
        </Card>
    );
}
