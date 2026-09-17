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
import type { GammaModule } from './modules.types';

interface ModuleProduct {
    /** Must match `plugin.properties#id` of the corresponding Gamma module (`gravitee-gamma-module-<id>`). */
    readonly id: string;
    readonly label: string;
    /** One line for the app switcher; the Home page card carries its own, longer description. */
    readonly tagline: string;
    /** When true, labels and icons stay in the catalog but the product is omitted from the console. */
    readonly hidden?: true;
}

/**
 * Gravitee products, in the order the app switcher and the home page present them: the products of the
 * gravitee.io Platform menu first, in its order, then the other modules.
 * `GET /organizations/{orgId}/modules` returns modules in plugin-registry (hash) order, so the UI owns the order.
 */
export const MODULE_CATALOG = [
    { id: 'aim', label: 'Agent Management', tagline: 'Govern AI agents, MCPs, and LLMs' },
    { id: 'apim', label: 'API Management', tagline: 'Design, deploy, and govern HTTP APIs' },
    { id: 'esm', label: 'Event Stream Management', tagline: 'Manage Kafka clusters, services, and event mesh' },
    { id: 'authz', label: 'Authorization Management', tagline: 'Fine-grained authorization policies' },
    { id: 'act', label: 'Guardian Agent', tagline: 'Build, equip, and watch the agents that guard your platform' },
    // Empty product; hide from the console until it ships.
    { id: 'portals', label: 'Developer Portals', tagline: 'Design and manage developer portal experiences', hidden: true },
    { id: 'edge', label: 'Edge Management', tagline: 'Monitor and manage Edge Daemons' },
    { id: 'platform', label: 'Platform Management', tagline: 'Apps, subscriptions, and usage' },
] as const satisfies readonly ModuleProduct[];

export type ModuleId = (typeof MODULE_CATALOG)[number]['id'];

const CATALOG_POSITION = new Map<string, number>(MODULE_CATALOG.map((product, index) => [product.id, index]));

export function findModuleProduct(moduleId: string): ModuleProduct | undefined {
    return MODULE_CATALOG.find(product => product.id === moduleId);
}

/** Modules missing from the catalog go last, by id, so a newly deployed module stays reachable at a stable position. */
export function orderByCatalog(modules: readonly GammaModule[]): GammaModule[] {
    const position = (moduleId: string) => CATALOG_POSITION.get(moduleId) ?? MODULE_CATALOG.length;
    return [...modules].sort((a, b) => position(a.id) - position(b.id) || a.id.localeCompare(b.id));
}

/** Drops catalog products marked hidden; uncatalogued modules stay so a newly deployed plugin remains reachable. */
export function excludeHiddenModules(modules: readonly GammaModule[]): GammaModule[] {
    return modules.filter(module => !findModuleProduct(module.id)?.hidden);
}

export function getModuleLabel(moduleId: string, fallbackName?: string): string {
    return findModuleProduct(moduleId)?.label ?? fallbackName ?? moduleId;
}
