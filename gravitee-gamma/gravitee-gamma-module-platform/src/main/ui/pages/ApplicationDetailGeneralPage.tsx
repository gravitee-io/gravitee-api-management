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
import { CircleCheckIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { ApplicationGeneralContent } from '../features/applications/components/general/ApplicationGeneralContent';
import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';

export interface ApplicationCreateLocationState {
    created?: boolean;
    applicationName?: string;
}

export function ApplicationDetailGeneralPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const { application, isLoading } = useApplicationDetailContext();
    const [createdMessage] = useState<string | null>(() => {
        const state = location.state as ApplicationCreateLocationState | null;
        return state?.created && state.applicationName ? state.applicationName : null;
    });

    useEffect(() => {
        if (location.state) {
            navigate('.', { replace: true, state: null });
        }
    }, [location.state, navigate]);

    if (isLoading) {
        return (
            <div className="space-y-6">
                {createdMessage ? (
                    <Alert className="border-success/30 bg-success/5">
                        <CircleCheckIcon className="size-4 text-success" aria-hidden />
                        <AlertDescription className="text-success">
                            Application &quot;{createdMessage}&quot; has been created successfully.
                        </AlertDescription>
                    </Alert>
                ) : null}
                <Skeleton className="h-10 w-64" />
                <Skeleton className="h-96 w-full" />
            </div>
        );
    }

    if (!application) {
        return <p className="text-sm text-muted-foreground">Application not found or you may not have access.</p>;
    }

    return (
        <div className="space-y-6">
            {createdMessage ? (
                <Alert className="border-success/30 bg-success/5">
                    <CircleCheckIcon className="size-4 text-success" aria-hidden />
                    <AlertDescription className="text-success">
                        Application &quot;{createdMessage}&quot; has been created successfully.
                    </AlertDescription>
                </Alert>
            ) : null}
            <ApplicationGeneralContent application={application} />
        </div>
    );
}
