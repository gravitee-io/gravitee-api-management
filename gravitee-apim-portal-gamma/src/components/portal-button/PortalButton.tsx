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
import { Button, type ButtonProps } from '@gravitee/graphene-core';

import styles from './PortalButton.module.scss';
import { toInstanceInlineStyle } from '../../features/theming/utils/instance-style';

export interface PortalButtonProps extends Omit<ButtonProps, 'variant'> {
    readonly instanceStyle?: Record<string, string>;
    readonly styleTargetVariant?: 'filled' | 'outlined' | 'ghost';
    readonly variant?: ButtonProps['variant'];
}

function mapThemeVariantToButton(
    styleTargetVariant: 'filled' | 'outlined' | 'ghost',
): NonNullable<ButtonProps['variant']> {
    switch (styleTargetVariant) {
        case 'outlined':
            return 'outline';
        case 'ghost':
            return 'ghost';
        case 'filled':
        default:
            return 'default';
    }
}

export function PortalButton({
    instanceStyle,
    styleTargetVariant = 'filled',
    className,
    variant,
    style,
    ...props
}: PortalButtonProps) {
    const buttonVariant = variant ?? mapThemeVariantToButton(styleTargetVariant);
    const instanceVars = toInstanceInlineStyle(instanceStyle);

    return (
        <Button
            data-style-target="button"
            data-style-variant={styleTargetVariant}
            className={`${styles.portalButton} ${className ?? ''}`}
            style={{ ...instanceVars, ...style }}
            variant={buttonVariant}
            {...props}
        />
    );
}
