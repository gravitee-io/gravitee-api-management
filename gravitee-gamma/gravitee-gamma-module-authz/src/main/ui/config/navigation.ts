import type { NavGroup } from '@gravitee/graphene-core';
import { Bot, Brain, Database, Globe, Radio, ShieldCheck, SlidersHorizontal, Users, Zap } from 'lucide-react';
import { ROUTES } from './routes';

/**
 * Sidebar IA. Trimmed to items wired to a real backend endpoint:
 * Policy Management · Policy structure.
 *
 * Authorization Dashboard removed during the canonical /gamma/authz migration —
 * its dependency on the module-local stats endpoint went away with the
 * duplicated REST layer. Re-introduce when a canonical stats projection
 * lands in authorization-api.
 */
export const NAV_GROUPS: NavGroup[] = [
    {
        label: 'Policy Management',
        items: [
            { key: 'mcps', title: ROUTES.mcps.label, icon: ShieldCheck },
            { key: 'agents', title: ROUTES.agents.label, icon: Bot },
            { key: 'llms', title: ROUTES.llms.label, icon: Brain },
            { key: 'apis', title: ROUTES.apis.label, icon: Globe },
            { key: 'events', title: ROUTES.events.label, icon: Radio },
            { key: 'custom-policies', title: ROUTES['custom-policies'].label, icon: SlidersHorizontal },
        ],
    },
    {
        label: 'Policy structure',
        items: [
            { key: 'entities', title: ROUTES.entities.label, icon: Users },
            { key: 'actions', title: ROUTES.actions.label, icon: Zap },
            { key: 'schema', title: ROUTES.schema.label, icon: Database },
        ],
    },
];
