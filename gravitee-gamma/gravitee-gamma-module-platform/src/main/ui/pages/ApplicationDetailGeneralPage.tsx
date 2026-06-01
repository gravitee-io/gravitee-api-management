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
import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { ApplicationCreatedBanner } from '../features/applications/components/general/ApplicationCreatedBanner';
import { ApplicationGeneralContent } from '../features/applications/components/general/ApplicationGeneralContent';
import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';
import { isApplicationCreatedNavigationState } from '../features/applications/utils/applicationDetailNavigation';

export function ApplicationDetailGeneralPage() {
    const { application, isLoading } = useApplicationDetailContext();
    const location = useLocation();
    const navigate = useNavigate();
    const [showCreatedBanner, setShowCreatedBanner] = useState(() => isApplicationCreatedNavigationState(location.state));

    useEffect(() => {
        if (!isApplicationCreatedNavigationState(location.state)) {
            return;
        }
        setShowCreatedBanner(true);
        navigate(location.pathname, { replace: true, state: null });
    }, [location.pathname, location.state, navigate]);

    if (isLoading) {
        return (
            <div className="space-y-6">
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
            {showCreatedBanner ? <ApplicationCreatedBanner /> : null}
            <ApplicationGeneralContent application={application} />
        </div>
    );
}
