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
import { cn } from '@gravitee/graphene-core';
import type { CSSProperties, ReactNode } from 'react';

import { DEFAULT_PAGE_WIDTH, PAGE_WIDTH_VALUES, type PageWidth } from '../editor/constants/page-width';
import styles from './HtmlPageWidthFrame.module.scss';

interface HtmlPageWidthFrameProps {
    readonly followLayoutWidth: boolean;
    readonly pageWidth?: PageWidth;
    readonly className?: string;
    readonly children: ReactNode;
}

export function HtmlPageWidthFrame({
    followLayoutWidth,
    pageWidth = DEFAULT_PAGE_WIDTH,
    className,
    children,
}: HtmlPageWidthFrameProps) {
    const frameClassName = cn(
        styles.frame,
        followLayoutWidth ? styles.constrained : styles.fullWidth,
        className,
    );
    const frameStyle = followLayoutWidth
        ? ({ '--page-width': PAGE_WIDTH_VALUES[pageWidth] } as CSSProperties)
        : undefined;

    return (
        <div className={frameClassName} style={frameStyle}>
            <div className={styles.content}>{children}</div>
        </div>
    );
}
