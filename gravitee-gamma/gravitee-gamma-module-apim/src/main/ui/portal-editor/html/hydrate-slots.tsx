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
import { CatalogView } from '../blocks/ApiCatalogBlock/CatalogView';
import { DEFAULT_TILE_TEMPLATE } from '../blocks/ApiCatalogBlock/tile-template';
import { ApplicationsView } from '../blocks/ApplicationsBlock/ApplicationsView';
import { SubscriptionFlowView } from '../blocks/SubscriptionFlowBlock/SubscriptionFlowView';
import { SubscriptionViewerView } from '../blocks/SubscriptionViewerBlock/SubscriptionViewerView';
import { GRAVITEE_SLOT_COMPONENTS } from './gravitee-slot-components';

function ApiCatalogSlot() {
    return <CatalogView tileTemplate={DEFAULT_TILE_TEMPLATE} viewMode="cards" clickable />;
}

function SubscriptionViewerSlot() {
    return <SubscriptionViewerView />;
}

function SubscriptionFlowSlot() {
    return <SubscriptionFlowView />;
}

function ApplicationsSlot() {
    return <ApplicationsView />;
}

const SLOT_COMPONENTS: Record<string, ComponentType> = Object.fromEntries(
    GRAVITEE_SLOT_COMPONENTS.map(component => {
        switch (component.id) {
            case 'api-catalog':
                return [component.id, ApiCatalogSlot];
            case 'subscription-viewer':
                return [component.id, SubscriptionViewerSlot];
            case 'subscription-flow':
                return [component.id, SubscriptionFlowSlot];
            case 'applications':
                return [component.id, ApplicationsSlot];
            default:
                return [component.id, () => null];
        }
    }),
);

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

        const updateSlots = () => {
            const nextSlots = scanSlots(container);
            setSlots(current => (slotsEqual(current, nextSlots) ? current : nextSlots));
        };

        updateSlots();

        const observer = new MutationObserver(updateSlots);
        observer.observe(container, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['data-gravitee-component'],
        });

        return () => observer.disconnect();
    }, [containerRef, enabled, htmlRevision]);

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
