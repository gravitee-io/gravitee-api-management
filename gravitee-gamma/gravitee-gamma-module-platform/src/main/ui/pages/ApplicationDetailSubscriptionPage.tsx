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
import { Skeleton } from '@gravitee/graphene-core';
import { useParams } from 'react-router-dom';

import { ApplicationSubscriptionDetailView } from '../features/applications/components/subscriptions/ApplicationSubscriptionDetailView';
import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';

export function ApplicationDetailSubscriptionPage() {
    const { subscriptionId } = useParams<{ subscriptionId: string }>();
    const { application, isLoading } = useApplicationDetailContext();

    if (isLoading) {
        return (
            <div className="space-y-5">
                <Skeleton className="h-8 w-48" />
                <Skeleton className="h-10 w-72" />
                <Skeleton className="h-96 w-full" />
            </div>
        );
    }

    if (!application || !subscriptionId) {
        return <p className="text-sm text-muted-foreground">Application not found or you may not have access.</p>;
    }

    return <ApplicationSubscriptionDetailView application={application} subscriptionId={subscriptionId} />;
}
