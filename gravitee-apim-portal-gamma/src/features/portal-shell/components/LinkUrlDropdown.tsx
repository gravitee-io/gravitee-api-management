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
import {
    Button,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuTrigger,
    Input,
} from '@gravitee/graphene-core';
import { useEffect, useRef, useState, type ReactNode } from 'react';

import type { PortalNavigationPage } from '../../portals/types';
import { getLinkDisplayUrl } from '../utils/user-menu-url';
import { NavLinkPagePicker } from './NavLinkPagePicker';
import styles from './LinkUrlDropdown.module.scss';

interface LinkUrlDropdownProps {
    readonly url: string;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly portalId?: string;
    readonly onUrlChange: (url: string) => void;
    readonly children: ReactNode;
    readonly open?: boolean;
    readonly onOpenChange?: (open: boolean) => void;
}

export function LinkUrlDropdown({
    url,
    portalPages,
    portalId,
    onUrlChange,
    children,
    open: controlledOpen,
    onOpenChange: controlledOnOpenChange,
}: LinkUrlDropdownProps) {
    const [internalOpen, setInternalOpen] = useState(false);
    const [customUrl, setCustomUrl] = useState('');
    const contentRef = useRef<HTMLDivElement>(null);

    const open = controlledOpen ?? internalOpen;
    const setOpen = controlledOnOpenChange ?? setInternalOpen;

    useEffect(() => {
        if (open) {
            setCustomUrl(getLinkDisplayUrl(url, portalPages, portalId));
        }
    }, [open, url, portalPages, portalId]);

    const keepFocusInsideContent = (target: EventTarget | null) => {
        return target instanceof Node && contentRef.current?.contains(target);
    };

    const handlePageSelect = (page: PortalNavigationPage) => {
        onUrlChange(page.slug);
        setOpen(false);
    };

    const handleCustomUrlApply = () => {
        const trimmed = customUrl.trim();
        if (trimmed) {
            onUrlChange(trimmed);
            setOpen(false);
        }
    };

    return (
        <DropdownMenu modal={false} open={open} onOpenChange={setOpen}>
            <DropdownMenuTrigger asChild>{children}</DropdownMenuTrigger>
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
                <div className={styles.content}>
                    <NavLinkPagePicker
                        pages={portalPages}
                        onSelect={handlePageSelect}
                        onCancel={() => setOpen(false)}
                    />
                    <div className={styles.divider} aria-hidden="true" />
                    <div className={styles.customUrlSection}>
                        <span className={styles.customUrlLabel}>Custom URL</span>
                        <Input
                            className={styles.customUrlInput}
                            placeholder="https://"
                            value={customUrl}
                            aria-label="Custom URL"
                            onChange={event => setCustomUrl(event.target.value)}
                            onKeyDown={event => {
                                if (event.key === 'Enter') {
                                    event.preventDefault();
                                    handleCustomUrlApply();
                                }
                            }}
                        />
                        <div className={styles.customUrlActions}>
                            <Button type="button" size="sm" variant="secondary" onClick={handleCustomUrlApply}>
                                Apply
                            </Button>
                        </div>
                    </div>
                </div>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
