import { AppLayout, AppSidebar, ContentHeader, useLayoutSlots } from '@gravitee/graphene-core';
import { Monitor } from 'lucide-react';
import type { ReactNode } from 'react';

const localDevApp = {
    key: 'module-local',
    label: 'AI Fleet',
    description: 'Local dev',
    icon: <Monitor size={20} />,
};

export function LocalDevShell({ children }: { readonly children: ReactNode }) {
    const { slots } = useLayoutSlots();
    const breadcrumbs = slots.breadcrumbs.length > 0 ? slots.breadcrumbs : [{ label: 'AI Fleet' }];

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
