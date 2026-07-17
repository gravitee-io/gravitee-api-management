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
import { DropdownMenu, DropdownMenuContent, DropdownMenuTrigger } from '@gravitee/graphene-core';
import { useRef, useState } from 'react';

import type { PortalNavigationPage } from '../../portals/types';
import { AddButton } from './AddButton';
import { NavLinkPagePicker } from './NavLinkPagePicker';

interface AddLinkDropdownProps {
    readonly portalPages: readonly PortalNavigationPage[];
    readonly onAddLinkFromPage: (page: PortalNavigationPage) => void;
    readonly ariaLabel?: string;
    readonly className?: string;
}

export function AddLinkDropdown({
    portalPages,
    onAddLinkFromPage,
    ariaLabel = 'Add footer link',
    className,
}: AddLinkDropdownProps) {
    const [open, setOpen] = useState(false);
    const contentRef = useRef<HTMLDivElement>(null);

    const keepFocusInsideContent = (target: EventTarget | null) => {
        return target instanceof Node && contentRef.current?.contains(target);
    };

    const handlePageSelect = (page: PortalNavigationPage) => {
        onAddLinkFromPage(page);
        setOpen(false);
    };

    return (
        <DropdownMenu open={open} onOpenChange={setOpen}>
            <DropdownMenuTrigger asChild>
                <AddButton aria-label={ariaLabel} className={className} />
            </DropdownMenuTrigger>
            <DropdownMenuContent
                ref={contentRef}
                align="start"
                className="w-auto min-w-72"
                onFocusOutside={event => {
                    if (keepFocusInsideContent(event.target)) {
                        event.preventDefault();
                    }
                }}
                onPointerDownOutside={event => {
                    if (keepFocusInsideContent(event.target)) {
                        event.preventDefault();
                    }
                }}
                onInteractOutside={event => {
                    if (keepFocusInsideContent(event.target)) {
                        event.preventDefault();
                    }
                }}
            >
                <NavLinkPagePicker
                    pages={portalPages}
                    onSelect={handlePageSelect}
                    onCancel={() => setOpen(false)}
                />
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
