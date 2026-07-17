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
import { useRef, useState } from 'react';
import { XIcon } from '@gravitee/graphene-core/icons';
import type { CSSProperties, KeyboardEvent, ReactNode } from 'react';
import { PortalNavItem } from '../../components/portal-nav-item/PortalNavItem';
import { InlineEdit } from '../../shared/components/InlineEdit';
import { UnpublishedNavIndicator } from './UnpublishedNavIndicator';
import styles from './NavItemButton.module.scss';

interface NavItemButtonProps {
    readonly navItemId?: string;
    readonly instanceStyle?: Record<string, string>;
    readonly label: string;
    readonly selected: boolean;
    readonly showDelete: boolean;
    readonly onSelect: () => void;
    readonly onDelete: () => void;
    readonly onLabelChange?: (label: string) => void;
    readonly variant?: 'header' | 'sidebar' | 'footer';
    readonly icon?: ReactNode;
    readonly title?: string;
    readonly style?: CSSProperties;
    readonly className?: string;
    readonly unpublished?: boolean;
}

export function NavItemButton({
    navItemId,
    instanceStyle,
    label,
    selected,
    showDelete,
    onSelect,
    onDelete,
    onLabelChange,
    variant = 'header',
    icon,
    title,
    style,
    className,
    unpublished = false,
}: NavItemButtonProps) {
    const isEditable = showDelete && Boolean(onLabelChange);
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

    const handleKeyDown = (event: KeyboardEvent<HTMLElement>) => {
        if (isRenaming) {
            return;
        }

        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            onSelect();
        }
    };

    const labelContent = isEditable ? (
        <span className={styles.labelArea}>
            <InlineEdit
                value={label}
                editable
                activateOn="doubleClick"
                onChange={onLabelChange!}
                onEditingChange={handleEditingChange}
                ariaLabel={`Edit ${label}`}
                className={styles.inlineLabel}
            />
        </span>
    ) : (
        <span className={styles.label}>{label}</span>
    );

    const containerStyle: CSSProperties = {
        ...style,
        ...(renameWidth != null
            ? { width: renameWidth, minWidth: renameWidth, maxWidth: renameWidth }
            : {}),
    };

    return (
        <div
            ref={containerRef}
            className={`${styles.navItemButton} ${selected ? styles.navItemSelected : ''} ${isRenaming ? styles.renaming : ''} ${unpublished ? styles.unpublished : ''}`}
            style={containerStyle}
            title={title}
        >
            <PortalNavItem
                instanceId={navItemId}
                instanceStyle={instanceStyle}
                selected={selected}
                layout={variant}
                className={className}
                onClick={onSelect}
                onKeyDown={handleKeyDown}
            >
                {icon ? <span className={styles.icon}>{icon}</span> : null}
                {labelContent}
                <UnpublishedNavIndicator show={unpublished} />
            </PortalNavItem>
            {showDelete && !isRenaming ? (
                <button
                    type="button"
                    className={styles.deleteButton}
                    aria-label={`Delete ${label}`}
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
