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
    ContextMenu,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuTrigger,
} from '@gravitee/graphene-core';
import { useEffect, useMemo, useRef, useState, type CSSProperties, type MouseEvent } from 'react';

import { PortalButton } from '../../components/portal-button/PortalButton';
import { usePortalPageOptional } from '../../portal-shell/context/PortalPageContext';
import { LinkUrlDropdown } from '../../portal-shell/components/LinkUrlDropdown';
import { usePortalPageNavigation } from '../../portal-shell/hooks/usePortalPageNavigation';
import { isExternalUrl } from '../../portal-shell/utils/link-target';
import { getPortalPages } from '../../portal-shell/utils/portal-pages';
import { parsePortalPageSlug } from '../../portal-shell/utils/user-menu-url';
import { useCustomizePanel } from '../../theming/components/CustomizePanelContext';
import { InlineEdit } from '../../shared/components/InlineEdit';
import styles from './ButtonBlock.module.scss';

function parseInstanceStyle(json: string): Record<string, string> {
    try {
        const parsed = JSON.parse(json);
        return typeof parsed === 'object' && parsed !== null ? parsed : {};
    } catch {
        return {};
    }
}

function mapAppearanceToVariant(appearance: string): 'filled' | 'outlined' | 'ghost' {
    if (appearance === 'outlined') {
        return 'outlined';
    }
    if (appearance === 'text') {
        return 'ghost';
    }
    return 'filled';
}

interface EditableButtonBlockProps {
    readonly label: string;
    readonly link: string;
    readonly appearance: string;
    readonly instanceStyleJson: string;
    readonly pickLinkOpen: string;
    readonly isEditable: boolean;
    readonly onUpdate: (patch: Record<string, string>) => void;
}

export function EditableButtonBlock({
    label,
    link,
    appearance,
    instanceStyleJson,
    pickLinkOpen,
    isEditable,
    onUpdate,
}: EditableButtonBlockProps) {
    const portalPage = usePortalPageOptional();
    const portalId = portalPage?.portalId;
    const portalPages = useMemo(() => getPortalPages(portalPage?.navItems ?? []), [portalPage?.navItems]);
    const { navigateToPageSlug } = usePortalPageNavigation(portalId);
    const customizePanel = useCustomizePanel();

    const instanceStyle = parseInstanceStyle(instanceStyleJson);
    const styleTargetVariant = mapAppearanceToVariant(appearance);

    const [isRenaming, setIsRenaming] = useState(false);
    const [renameWidth, setRenameWidth] = useState<number | undefined>();
    const [linkPickerOpen, setLinkPickerOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);
    const contextMenuPositionRef = useRef({ x: 0, y: 0 });
    const ignoreLinkPickerCloseUntilRef = useRef(0);

    const scheduleAfterContextMenu = (action: () => void) => {
        window.setTimeout(action, 0);
    };

    const handleLinkPickerOpenChange = (open: boolean) => {
        if (open) {
            ignoreLinkPickerCloseUntilRef.current = performance.now() + 200;
            setLinkPickerOpen(true);
            return;
        }

        if (performance.now() < ignoreLinkPickerCloseUntilRef.current) {
            return;
        }

        setLinkPickerOpen(false);
    };

    useEffect(() => {
        if (pickLinkOpen !== 'true' || !isEditable) {
            return;
        }

        const timeoutId = window.setTimeout(() => {
            handleLinkPickerOpenChange(true);
            onUpdate({ pickLinkOpen: 'false' });
        }, 0);

        return () => window.clearTimeout(timeoutId);
    }, [pickLinkOpen, isEditable, onUpdate]);

    const handleEditingChange = (editing: boolean) => {
        if (editing && containerRef.current) {
            setRenameWidth(containerRef.current.offsetWidth);
        } else {
            setRenameWidth(undefined);
        }
        setIsRenaming(editing);
    };

    const handleLabelDoubleClick = (event: MouseEvent) => {
        event.stopPropagation();
    };

    const handleContextMenu = (event: MouseEvent) => {
        event.stopPropagation();
        contextMenuPositionRef.current = { x: event.clientX, y: event.clientY };
    };

    const handlePreviewClick = () => {
        if (isExternalUrl(link)) {
            window.open(link, '_blank', 'noopener,noreferrer');
            return;
        }

        const slug = parsePortalPageSlug(link, portalPages, portalId);
        if (slug) {
            navigateToPageSlug(slug);
        }
    };

    const containerStyle: CSSProperties = {
        ...(renameWidth != null
            ? { width: renameWidth, minWidth: renameWidth, maxWidth: renameWidth }
            : {}),
    };

    const labelContent = isEditable ? (
        <span className={styles.labelArea} onDoubleClick={handleLabelDoubleClick}>
            <InlineEdit
                value={label}
                editable
                activateOn="doubleClick"
                onChange={newLabel => onUpdate({ label: newLabel })}
                onEditingChange={handleEditingChange}
                ariaLabel={`Edit ${label}`}
                className={styles.inlineLabel}
            />
        </span>
    ) : (
        label
    );

    const buttonElement = (
        <PortalButton
            type="button"
            styleTargetVariant={styleTargetVariant}
            instanceStyle={instanceStyle}
            className={styles.portalButton}
            onClick={event => {
                if (isEditable) {
                    event.preventDefault();
                    return;
                }
                handlePreviewClick();
            }}
            onPointerDown={isEditable ? event => event.stopPropagation() : undefined}
        >
            {labelContent}
        </PortalButton>
    );

    if (!isEditable) {
        return (
            <div className={styles.wrapper} ref={containerRef}>
                {buttonElement}
            </div>
        );
    }

    const openStylePanel = () => {
        const styleTarget = containerRef.current?.querySelector('[data-style-target="button"]') as HTMLElement | null;
        if (styleTarget && customizePanel) {
            customizePanel.openCustomizePanel(styleTarget, contextMenuPositionRef.current);
        }
    };

    const handleEditSelect = () => {
        scheduleAfterContextMenu(() => handleLinkPickerOpenChange(true));
    };

    const handleStyleSelect = () => {
        scheduleAfterContextMenu(openStylePanel);
    };

    return (
        <div
            ref={containerRef}
            className={`${styles.wrapper} ${styles.editable} ${isRenaming ? styles.renaming : ''}`}
            style={containerStyle}
            onContextMenu={handleContextMenu}
        >
            <ContextMenu>
                <ContextMenuTrigger asChild>
                    <div className={styles.buttonTrigger}>
                        <LinkUrlDropdown
                            url={link}
                            portalPages={portalPages}
                            portalId={portalId}
                            openOnClick={false}
                            open={linkPickerOpen}
                            onOpenChange={handleLinkPickerOpenChange}
                            onUrlChange={url => onUpdate({ link: url })}
                        >
                            {buttonElement}
                        </LinkUrlDropdown>
                    </div>
                </ContextMenuTrigger>
                <ContextMenuContent>
                    <ContextMenuItem onSelect={handleEditSelect}>Edit</ContextMenuItem>
                    <ContextMenuItem onSelect={handleStyleSelect}>Style</ContextMenuItem>
                </ContextMenuContent>
            </ContextMenu>
        </div>
    );
}
