import type { NavGroup } from '@gravitee/graphene-core';
import { Brain, Globe, ShieldCheck, SlidersHorizontal } from 'lucide-react';
import { ROUTES } from './routes';

/**
 * Sidebar IA. Exposes Policy Management only. Policy structure
 * (entities · actions · schema) is introduced incrementally by
 * follow-up PRs in the gamma-ui stack.
 */
export const NAV_GROUPS: NavGroup[] = [
    {
        label: 'Policy Management',
        items: [
            { key: 'mcps', title: ROUTES.mcps.label, icon: ShieldCheck },
            { key: 'llms', title: ROUTES.llms.label, icon: Brain },
            { key: 'apis', title: ROUTES.apis.label, icon: Globe },
            { key: 'custom-policies', title: ROUTES['custom-policies'].label, icon: SlidersHorizontal },
        ],
    },
];
