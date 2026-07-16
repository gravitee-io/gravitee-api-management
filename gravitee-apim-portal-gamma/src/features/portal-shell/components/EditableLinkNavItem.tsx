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
import { useRef, useState, type CSSProperties, type KeyboardEvent, type MouseEvent, type ReactNode } from 'react';
import { XIcon } from '@gravitee/graphene-core/icons';

import { PortalNavItem } from '../../../components/portal-nav-item/PortalNavItem';
import type { PortalNavigationLink, PortalNavigationPage } from '../../portals/types';
import { InlineEdit } from '../../../shared/components/InlineEdit';
import { getNavTypeIcon } from '../utils/nav-type-icons';
import { LinkUrlDropdown } from './LinkUrlDropdown';
import { UnpublishedNavIndicator } from './UnpublishedNavIndicator';
import navItemStyles from './NavItemButton.module.scss';
import styles from './EditableLinkNavItem.module.scss';

interface EditableLinkNavItemProps {
    readonly item: PortalNavigationLink;
    readonly instanceStyle?: Record<string, string>;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly portalId: string;
    readonly selected?: boolean;
    readonly showDelete: boolean;
    readonly variant: 'header' | 'sidebar' | 'footer';
    readonly icon?: ReactNode;
    readonly title?: string;
    readonly className?: string;
    readonly style?: CSSProperties;
    readonly onUpdate: (patch: { title?: string; url?: string }) => void;
    readonly onDelete: () => void;
    readonly unpublished?: boolean;
}

export function EditableLinkNavItem({
    item,
    instanceStyle,
    portalPages,
    portalId,
    selected = false,
    showDelete,
    variant,
    icon,
    title: tooltip,
    className,
    style,
    onUpdate,
    onDelete,
    unpublished = false,
}: EditableLinkNavItemProps) {
    const [isRenaming, setIsRenaming] = useState(false);
    const [renameWidth, setRenameWidth] = useState<number | undefined>();
    const containerRef = useRef<HTMLDivElement>(null);

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

    const containerStyle: CSSProperties = {
        ...style,
        ...(renameWidth != null
            ? { width: renameWidth, minWidth: renameWidth, maxWidth: renameWidth }
            : {}),
    };

    const linkIcon = icon ?? getNavTypeIcon('LINK');

    const labelContent = (
        <span className={navItemStyles.labelArea} onDoubleClick={handleLabelDoubleClick}>
            <InlineEdit
                value={item.title}
                editable
                activateOn="doubleClick"
                onChange={title => onUpdate({ title })}
                onEditingChange={handleEditingChange}
                ariaLabel={`Edit ${item.title}`}
                className={navItemStyles.inlineLabel}
            />
        </span>
    );

    const triggerButton = (
        <PortalNavItem
            asDiv
            instanceId={item.id}
            instanceStyle={instanceStyle}
            selected={selected}
            layout={variant}
            className={`${className ?? ''} ${styles.linkTrigger}`}
            onKeyDown={(event: KeyboardEvent<HTMLElement>) => {
                if (isRenaming) {
                    return;
                }
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                }
            }}
        >
            {linkIcon ? <span className={navItemStyles.icon}>{linkIcon}</span> : null}
            {labelContent}
            <UnpublishedNavIndicator show={unpublished} />
        </PortalNavItem>
    );

    return (
        <div
            ref={containerRef}
            className={`${navItemStyles.navItemButton} ${selected ? navItemStyles.navItemSelected : ''} ${isRenaming ? navItemStyles.renaming : ''} ${variant === 'footer' ? styles.footerItem : ''} ${unpublished ? navItemStyles.unpublished : ''}`}
            style={containerStyle}
            title={tooltip}
        >
            <LinkUrlDropdown
                url={item.url}
                portalPages={portalPages}
                portalId={portalId}
                onUrlChange={url => onUpdate({ url })}
            >
                {triggerButton}
            </LinkUrlDropdown>
            {showDelete && !isRenaming ? (
                <button
                    type="button"
                    className={navItemStyles.deleteButton}
                    aria-label={`Delete ${item.title}`}
                    onClick={event => {
                        event.stopPropagation();
                        onDelete();
                    }}
                >
                    <XIcon className="size-3.5" aria-hidden="true" />
                </button>
            ) : null}
        </div>
    );
}

interface PreviewLinkNavItemProps {
    readonly navItemId?: string;
    readonly instanceStyle?: Record<string, string>;
    readonly label: string;
    readonly selected: boolean;
    readonly variant: 'header' | 'sidebar' | 'footer';
    readonly icon?: ReactNode;
    readonly className?: string;
    readonly onSelect: () => void;
}

export function PreviewLinkNavItem({
    navItemId,
    instanceStyle,
    label,
    selected,
    variant,
    icon,
    className,
    onSelect,
}: PreviewLinkNavItemProps) {
    const linkIcon = icon ?? getNavTypeIcon('LINK');

    return (
        <div className={`${navItemStyles.navItemButton} ${selected ? navItemStyles.navItemSelected : ''}`}>
            <PortalNavItem
                instanceId={navItemId}
                instanceStyle={instanceStyle}
                selected={selected}
                layout={variant}
                className={className}
                onClick={onSelect}
            >
                {linkIcon ? <span className={navItemStyles.icon}>{linkIcon}</span> : null}
                <span className={navItemStyles.label}>{label}</span>
            </PortalNavItem>
        </div>
    );
}
