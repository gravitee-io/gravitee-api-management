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
    Alert,
    AlertDescription,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Empty,
    EmptyHeader,
    EmptyTitle,
    Skeleton,
} from '@gravitee/graphene-core';

import { IntegrationDirectMembersTable } from './IntegrationDirectMembersTable';
import { useIntegrationMembers } from '../hooks/useIntegrationMembers';
import { integrationMembersLoadErrorMessage } from '../utils/integrationMembersLoadErrorMessage';

export function IntegrationDirectMembers({ integrationId }: Readonly<{ integrationId: string }>) {
    const { data: members, isLoading, isError, error } = useIntegrationMembers(integrationId);

    function renderContent() {
        if (isError) {
            return (
                <Alert variant="destructive">
                    <AlertDescription>{integrationMembersLoadErrorMessage(error)}</AlertDescription>
                </Alert>
            );
        }

        if (isLoading || !members) {
            return <Skeleton className="h-12 rounded-lg" />;
        }

        if (members.length === 0) {
            return (
                <Empty>
                    <EmptyHeader>
                        <EmptyTitle>No direct members</EmptyTitle>
                    </EmptyHeader>
                </Empty>
            );
        }

        return <IntegrationDirectMembersTable members={members} />;
    }

    return (
        <Card>
            <CardHeader className="pb-3">
                <CardTitle className="text-base">Direct Members</CardTitle>
            </CardHeader>
            <CardContent>{renderContent()}</CardContent>
        </Card>
    );
}
