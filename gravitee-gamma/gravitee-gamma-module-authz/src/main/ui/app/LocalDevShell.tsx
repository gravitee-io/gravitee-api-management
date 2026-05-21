import { AppLayout, AppSidebar, ContentHeader, useLayoutSlots } from '@gravitee/graphene-core';
import { Boxes } from 'lucide-react';
import type { ReactNode } from 'react';

const localDevApp = {
    key: 'module-local',
    label: 'Authorization',
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
