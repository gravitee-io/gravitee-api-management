import { LayoutSlotsProvider } from '@gravitee/graphene-core';
import { BrowserRouter } from 'react-router-dom';

import { AppRoutes } from './AppRoutes';
import { LocalDevShell } from './LocalDevShell';

export default function LocalDevRoot() {
    return (
        <BrowserRouter>
            <LayoutSlotsProvider>
                <LocalDevShell>
                    <AppRoutes />
                </LocalDevShell>
            </LayoutSlotsProvider>
        </BrowserRouter>
    );
}
