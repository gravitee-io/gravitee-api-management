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
import { Button } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { Link } from 'react-router-dom';

import { PORTALS_ROUTE_CONFIG, usePortalsNavigation, type PortalsStubNavKey } from '../config/navigation';

interface ModuleComingSoonPageProps {
    readonly navKey: PortalsStubNavKey;
}

export function ModuleComingSoonPage({ navKey }: ModuleComingSoonPageProps) {
    const { homePath } = usePortalsNavigation();
    const title = PORTALS_ROUTE_CONFIG.routes[navKey].label;

    return (
        <div className="flex flex-1 flex-col items-center justify-center px-6 py-16 text-center" role="main">
            <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
            <p className="mt-2 max-w-md text-sm text-muted-foreground">
                This section is coming soon. Check back later or return to Overview.
            </p>
            <Button className="mt-8 gap-1.5" asChild>
                <Link to={homePath}>
                    <ArrowLeftIcon className="size-4" aria-hidden="true" />
                    Back to Overview
                </Link>
            </Button>
        </div>
    );
}
