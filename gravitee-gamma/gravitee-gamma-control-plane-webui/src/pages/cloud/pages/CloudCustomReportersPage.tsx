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
    Badge,
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    toast,
} from '@gravitee/graphene-core';
import { InfoIcon, PencilIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import {
    CLOUD_CUSTOM_REPORTERS_MOCK,
    CUSTOM_REPORTERS_DOCS_URL,
    type CustomReporter,
    type CustomReporterStatus,
} from '../cloud.config';

function ReporterStatusBadge({ status }: { readonly status: CustomReporterStatus }) {
    if (status === 'active') {
        return (
            <Badge className="gap-1 border-transparent bg-success/10 text-success hover:bg-success/10">
                Active
                <InfoIcon className="size-3.5 opacity-80" aria-label="Reporter is linked and active" />
            </Badge>
        );
    }

    return (
        <Badge variant="secondary" className="gap-1">
            Not Linked
            <InfoIcon className="size-3.5 opacity-80" aria-label="Reporter is not linked to a gateway" />
        </Badge>
    );
}

export function CloudCustomReportersPage() {
    const [reporters, setReporters] = useState<CustomReporter[]>(() => [...CLOUD_CUSTOM_REPORTERS_MOCK]);

    const handleToggle = useCallback((id: string, enabled: boolean) => {
        setReporters(current => current.map(reporter => (reporter.id === id ? { ...reporter, enabled } : reporter)));
    }, []);

    const handleCreate = useCallback(() => {
        toast.info('Create Custom Reporter will be available when the Cloud API is connected.');
    }, []);

    const handleEdit = useCallback((name: string) => {
        toast.info(`Edit "${name}" will be available when the Cloud API is connected.`);
    }, []);

    const handleDelete = useCallback((name: string) => {
        toast.info(`Delete "${name}" will be available when the Cloud API is connected.`);
    }, []);

    return (
        <div className="max-w-screen-xl space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-bold tracking-tight">Custom Reporters</h1>
                <p className="text-sm text-muted-foreground">Manage and configure your custom API gateway reporters.</p>
            </div>

            <Card>
                <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <CardTitle className="text-base">Your Custom Reporters</CardTitle>
                    <div className="flex flex-wrap gap-2">
                        <Button variant="outline" asChild>
                            <a href={CUSTOM_REPORTERS_DOCS_URL} target="_blank" rel="noopener noreferrer">
                                View documentation
                            </a>
                        </Button>
                        <Button onClick={handleCreate} data-testid="create-custom-reporter">
                            <PlusIcon aria-hidden />
                            Create Custom Reporter
                        </Button>
                    </div>
                </CardHeader>
                <CardContent>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Name</TableHead>
                                <TableHead>Type</TableHead>
                                <TableHead>Configuration</TableHead>
                                <TableHead>Output Format</TableHead>
                                <TableHead>Status</TableHead>
                                <TableHead className="w-32 text-right">Actions</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {reporters.map(reporter => (
                                <TableRow key={reporter.id} data-testid={`custom-reporter-row-${reporter.id}`}>
                                    <TableCell className="font-medium">{reporter.name}</TableCell>
                                    <TableCell>{reporter.type}</TableCell>
                                    <TableCell className="max-w-xs truncate" title={reporter.configuration}>
                                        {reporter.configuration}
                                    </TableCell>
                                    <TableCell>
                                        <Badge variant="secondary">{reporter.outputFormat}</Badge>
                                    </TableCell>
                                    <TableCell>
                                        <ReporterStatusBadge status={reporter.status} />
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex items-center justify-end gap-1">
                                            <Switch
                                                checked={reporter.enabled}
                                                onCheckedChange={checked => handleToggle(reporter.id, checked)}
                                                aria-label={`Toggle ${reporter.name}`}
                                            />
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon-sm"
                                                onClick={() => handleEdit(reporter.name)}
                                                aria-label={`Edit ${reporter.name}`}
                                            >
                                                <PencilIcon aria-hidden />
                                            </Button>
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon-sm"
                                                onClick={() => handleDelete(reporter.name)}
                                                aria-label={`Delete ${reporter.name}`}
                                            >
                                                <Trash2Icon aria-hidden />
                                            </Button>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </CardContent>
            </Card>
        </div>
    );
}
