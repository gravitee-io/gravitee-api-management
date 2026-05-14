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
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';

import type { AlertHistoryPage } from '../../features/apis/types/api';

export interface HistoryTabProps {
    historyPage: AlertHistoryPage | undefined;
}

export function HistoryTab({ historyPage }: HistoryTabProps) {
    return (
        <div className="mt-6">
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">History</CardTitle>
                    <CardDescription>Events history for this alert</CardDescription>
                </CardHeader>
                <CardContent>
                    {historyPage && historyPage.content.length > 0 ? (
                        <div className="rounded-lg border">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead>Date</TableHead>
                                        <TableHead>Message</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {historyPage.content.map(evt => (
                                        <TableRow key={evt.id}>
                                            <TableCell className="text-sm">{new Date(evt.createdAt).toLocaleString()}</TableCell>
                                            <TableCell className="text-sm text-muted-foreground">{evt.message}</TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </div>
                    ) : (
                        <div className="py-8 text-center text-sm text-muted-foreground">No data to display.</div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}
