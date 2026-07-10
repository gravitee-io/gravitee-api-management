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
import { type ComponentType, type RefObject, useLayoutEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { CatalogView } from '../../blocks/ApiCatalogBlock/CatalogView';
import { DEFAULT_TILE_TEMPLATE } from '../../blocks/ApiCatalogBlock/tile-template';

function ApiCatalogSlot() {
    return <CatalogView tileTemplate={DEFAULT_TILE_TEMPLATE} viewMode="cards" clickable />;
}

const SLOT_COMPONENTS: Record<string, ComponentType> = {
    'api-catalog': ApiCatalogSlot,
};

interface SlotEntry {
    readonly element: HTMLElement;
    readonly componentName: string;
    readonly key: string;
}

function scanSlots(container: HTMLElement): SlotEntry[] {
    return Array.from(container.querySelectorAll<HTMLElement>('[data-gravitee-component]'))
        .map((element, index) => {
            const componentName = element.getAttribute('data-gravitee-component') ?? '';
            return {
                element,
                componentName,
                key: `${componentName}-${index}`,
            };
        })
        .filter(entry => entry.componentName && SLOT_COMPONENTS[entry.componentName]);
}

function slotsEqual(current: SlotEntry[], next: SlotEntry[]): boolean {
    return (
        current.length === next.length
        && current.every(
            (entry, index) =>
                entry.element === next[index]?.element
                && entry.componentName === next[index]?.componentName
                && entry.key === next[index]?.key,
        )
    );
}

interface HtmlSlotHydratorProps {
    readonly containerRef: RefObject<HTMLElement | null>;
    readonly enabled: boolean;
    readonly htmlRevision: string;
}

export function HtmlSlotHydrator({ containerRef, enabled, htmlRevision }: HtmlSlotHydratorProps) {
    const [slots, setSlots] = useState<SlotEntry[]>([]);

    useLayoutEffect(() => {
        if (!enabled) {
            setSlots([]);
            return;
        }

        const container = containerRef.current;
        if (!container) {
            setSlots([]);
            return;
        }

        const nextSlots = scanSlots(container);
        setSlots(current => (slotsEqual(current, nextSlots) ? current : nextSlots));
    }, [containerRef, enabled, htmlRevision]);

    useLayoutEffect(() => {
        if (!enabled) {
            return;
        }

        setSlots(current => {
            if (current.length > 0 && current.every(entry => entry.element.isConnected)) {
                return current;
            }

            const container = containerRef.current;
            if (!container) {
                return current;
            }

            const nextSlots = scanSlots(container);
            return slotsEqual(current, nextSlots) ? current : nextSlots;
        });
    });

    if (!enabled) {
        return null;
    }

    return (
        <>
            {slots.map(({ element, componentName, key }) => {
                const Component = SLOT_COMPONENTS[componentName];
                return createPortal(<Component />, element, key);
            })}
        </>
    );
}
