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
import { Alert, AlertDescription, Skeleton } from '@gravitee/graphene-core';
import { useParams } from 'react-router-dom';

import { ApplicationOverviewChecklist } from '../features/applications/components/overview/ApplicationOverviewChecklist';
import { ApplicationOverviewSnapshot } from '../features/applications/components/overview/ApplicationOverviewSnapshot';
import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';
import { useApplicationOverviewData } from '../features/applications/hooks/useApplicationOverviewData';

export function ApplicationDetailOverviewPage() {
    const { applicationId } = useParams<{ applicationId: string }>();
    const { application, isLoading } = useApplicationDetailContext();
    const overviewData = useApplicationOverviewData(applicationId);

    if (isLoading) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-10 w-64" />
                <Skeleton className="h-72 w-full" />
                <Skeleton className="h-32 w-full" />
            </div>
        );
    }

    if (!application) {
        return <p className="text-sm text-muted-foreground">Application not found or you may not have access.</p>;
    }

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Overview</h1>
                <p className="text-sm text-muted-foreground">Setup progress and snapshot for {application.name}.</p>
            </div>

            {overviewData.isError ? (
                <Alert variant="destructive">
                    <AlertDescription>Some overview data could not be loaded. Please refresh and try again.</AlertDescription>
                </Alert>
            ) : null}

            <ApplicationOverviewChecklist application={application} overviewData={overviewData} />

            <ApplicationOverviewSnapshot overviewData={overviewData} />
        </div>
    );
}
