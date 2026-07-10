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
import { type ComponentType } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { CatalogView } from '../../blocks/ApiCatalogBlock/CatalogView';
import { DEFAULT_TILE_TEMPLATE } from '../../blocks/ApiCatalogBlock/tile-template';

function ApiCatalogSlot() {
    return <CatalogView tileTemplate={DEFAULT_TILE_TEMPLATE} viewMode="cards" clickable />;
}

const SLOT_COMPONENTS: Record<string, ComponentType> = {
    'api-catalog': ApiCatalogSlot,
};

export function hydrateSlots(container: HTMLElement): () => void {
    const slots = container.querySelectorAll('[data-gravitee-component]');
    const roots: Root[] = [];

    for (const slot of Array.from(slots)) {
        const componentName = slot.getAttribute('data-gravitee-component');
        if (!componentName) continue;

        const Component = SLOT_COMPONENTS[componentName];
        if (!Component) continue;

        const root = createRoot(slot);
        root.render(<Component />);
        roots.push(root);
    }

    return () => roots.forEach(r => r.unmount());
}
