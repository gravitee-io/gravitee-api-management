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
    Button,
    Card,
    CardContent,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    toast,
} from '@gravitee/graphene-core';
import { PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useEnvHrid } from '../../../features/environment/environment.utils';
import { CLOUD_ACCOUNT_MEMBERS_MOCK, type AccountMember } from '../cloud.config';
import { cloudSettingsNavPath } from '../cloud-settings-navigation';

export function CloudMembersPage() {
    const navigate = useNavigate();
    const envHrid = useEnvHrid();
    const [members, setMembers] = useState<readonly AccountMember[]>(CLOUD_ACCOUNT_MEMBERS_MOCK);

    const handleAdd = useCallback(() => {
        navigate(cloudSettingsNavPath(envHrid, 'invite-member'));
    }, [envHrid, navigate]);

    const handleEdit = useCallback((name: string) => {
        toast.info(`Edit "${name}" will be available when the Cloud API is connected.`);
    }, []);

    const handleDelete = useCallback((memberId: string) => {
        setMembers(current => current.filter(member => member.id !== memberId));
        toast.success('Member has been removed');
    }, []);

    return (
        <div className="max-w-screen-xl space-y-6">
            <h1 className="text-2xl font-bold tracking-tight">Account members</h1>

            <Card>
                <CardContent className="p-0">
                    <Table>
                        <TableHeader>
                            <TableRow className="bg-muted/40 hover:bg-muted/40">
                                <TableHead className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Name</TableHead>
                                <TableHead className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Email</TableHead>
                                <TableHead className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Role</TableHead>
                                <TableHead className="w-24" />
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {members.map(member => (
                                <TableRow key={member.id} data-testid={`account-member-row-${member.id}`}>
                                    <TableCell className="font-medium">{member.name}</TableCell>
                                    <TableCell className="text-muted-foreground">{member.email}</TableCell>
                                    <TableCell className="text-muted-foreground">{member.role}</TableCell>
                                    <TableCell className="text-right">
                                        {!member.readonly && (
                                            <div className="flex items-center justify-end gap-1">
                                                <Button
                                                    type="button"
                                                    variant="ghost"
                                                    size="icon-sm"
                                                    onClick={() => handleEdit(member.name)}
                                                    aria-label={`Edit ${member.name}`}
                                                >
                                                    <PencilIcon aria-hidden />
                                                </Button>
                                                <Button
                                                    type="button"
                                                    variant="ghost"
                                                    size="icon-sm"
                                                    onClick={() => handleDelete(member.id)}
                                                    aria-label={`Delete ${member.name}`}
                                                >
                                                    <Trash2Icon aria-hidden />
                                                </Button>
                                            </div>
                                        )}
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>

                    <div className="flex justify-end border-t px-6 py-4">
                        <Button onClick={handleAdd} data-testid="add-member-btn">
                            Add
                        </Button>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
