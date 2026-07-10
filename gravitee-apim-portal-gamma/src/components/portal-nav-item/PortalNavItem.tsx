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
import type { CSSProperties, KeyboardEvent, ReactNode } from 'react';

import { toInstanceInlineStyle } from '../../features/theming/utils/instance-style';
import styles from './PortalNavItem.module.scss';

type PortalNavItemLayout = 'header' | 'sidebar' | 'footer';

interface PortalNavItemBaseProps {
    readonly selected?: boolean;
    readonly layout?: PortalNavItemLayout;
    readonly className?: string;
    readonly style?: CSSProperties;
    readonly instanceId?: string;
    readonly instanceStyle?: Record<string, string>;
    readonly children: ReactNode;
    readonly onClick?: () => void;
    readonly onKeyDown?: (event: KeyboardEvent<HTMLElement>) => void;
}

export type PortalNavItemProps = PortalNavItemBaseProps & (
    | { readonly asDiv?: false }
    | { readonly asDiv: true }
);

function buildClassName(layout: PortalNavItemLayout, className?: string): string {
    const layoutClassName = layout === 'header'
        ? styles.header
        : layout === 'sidebar'
            ? styles.sidebar
            : styles.footer;

    return [
        styles.portalNavItem,
        layoutClassName,
        layout === 'sidebar' ? styles.sidebarAlign : undefined,
        className,
    ]
        .filter(Boolean)
        .join(' ');
}

function mergeStyles(
    style?: CSSProperties,
    instanceStyle?: Record<string, string>,
): CSSProperties | undefined {
    const instanceVars = toInstanceInlineStyle(instanceStyle);
    if (!style && !instanceVars) {
        return undefined;
    }
    return { ...instanceVars, ...style };
}

export function PortalNavItem({
    selected = false,
    layout = 'header',
    asDiv = false,
    className,
    style,
    instanceId,
    instanceStyle,
    children,
    onClick,
    onKeyDown,
}: PortalNavItemProps) {
    const classNames = buildClassName(layout, className);
    const mergedStyle = mergeStyles(style, instanceStyle);

    if (asDiv) {
        return (
            <div
                role="button"
                tabIndex={0}
                data-style-target="nav-item"
                data-style-part={selected ? 'selected' : 'default'}
                data-instance-id={instanceId}
                data-active={selected || undefined}
                aria-current={selected ? 'page' : undefined}
                className={classNames}
                style={mergedStyle}
                onClick={onClick}
                onKeyDown={event => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        onClick?.();
                    }
                    onKeyDown?.(event);
                }}
            >
                {children}
            </div>
        );
    }

    return (
        <button
            type="button"
            data-style-target="nav-item"
            data-style-part={selected ? 'selected' : 'default'}
            data-instance-id={instanceId}
            data-active={selected || undefined}
            aria-current={selected ? 'page' : undefined}
            className={classNames}
            style={mergedStyle}
            onClick={onClick}
            onKeyDown={onKeyDown}
        >
            {children}
        </button>
    );
}
