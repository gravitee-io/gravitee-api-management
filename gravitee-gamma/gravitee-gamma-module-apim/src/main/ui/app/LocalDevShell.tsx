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
import { AppLayout, AppSidebar, ContentHeader, useLayoutSlots } from '@gravitee/graphene';
import { Boxes } from 'lucide-react';
import type { ReactNode } from 'react';

const localDevApp = {
    key: 'module-local',
    label: 'API Management',
    description: 'Local dev',
    icon: <Boxes size={20} />,
};

/** Minimal app chrome for standalone `nx serve` only; the host provides the real shell when federated. */
export function LocalDevShell({ children }: { readonly children: ReactNode }) {
    const { slots } = useLayoutSlots();
    const breadcrumbs = slots.breadcrumbs.length > 0 ? slots.breadcrumbs : [{ label: 'APIM' }];

    return (
        <AppLayout
            defaultSidebarMode="hover-expand"
            defaultTheme="system"
            fullHeight
            sidebar={<AppSidebar apps={[localDevApp]} activeAppKey={localDevApp.key} renderNavigation={() => slots.navigation} />}
            subheader={<ContentHeader breadcrumbs={breadcrumbs} />}
        >
            {children}
        </AppLayout>
    );
}
