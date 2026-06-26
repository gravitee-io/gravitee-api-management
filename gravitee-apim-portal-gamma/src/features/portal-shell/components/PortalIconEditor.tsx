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
import { Button, Tooltip, TooltipContent, TooltipTrigger } from '@gravitee/graphene-core';
import { RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useRef } from 'react';

import { uploadFile } from '../../editor/utils/upload';
import styles from './PortalIconEditor.module.scss';

function PortalIconGlyph() {
    return (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="10" />
            <line x1="2" y1="12" x2="22" y2="12" />
            <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
        </svg>
    );
}

interface PortalIconEditorProps {
    readonly portalIconUrl: string;
    readonly editable: boolean;
    readonly onChange?: (portalIconUrl: string) => void;
    readonly className?: string;
}

export function PortalIconEditor({ portalIconUrl = '', editable, onChange, className }: PortalIconEditorProps) {
    const fileInputRef = useRef<HTMLInputElement>(null);
    const hasCustomIcon = portalIconUrl.length > 0;

    const iconContent = hasCustomIcon ? (
        <img src={portalIconUrl} alt="Portal" className={styles.icon} />
    ) : (
        <div className={styles.placeholder} aria-label="Portal icon">
            <PortalIconGlyph />
        </div>
    );

    if (!editable) {
        return (
            <div className={`${styles.wrapper} ${className ?? ''}`}>
                <span className={styles.frame}>{iconContent}</span>
            </div>
        );
    }

    return (
        <div className={`${styles.wrapper} portal-editable-region ${className ?? ''}`}>
            <button
                type="button"
                className={`${styles.frame} ${styles.button}`}
                aria-label="Change portal icon"
                onClick={() => fileInputRef.current?.click()}
            >
                {iconContent}
            </button>
            {hasCustomIcon && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            className={styles.resetOverlay}
                            aria-label="Reset to default"
                            tabIndex={-1}
                            onClick={event => {
                                event.stopPropagation();
                                onChange?.('');
                            }}
                        >
                            <RefreshCwIcon className="size-3.5" aria-hidden />
                        </Button>
                    </TooltipTrigger>
                    <TooltipContent>Reset to default</TooltipContent>
                </Tooltip>
            )}
            <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className={styles.hiddenFileInput}
                onChange={event => {
                    const file = event.target.files?.[0];
                    if (!file || !onChange) {
                        return;
                    }

                    void uploadFile(file).then(onChange);
                    event.target.value = '';
                }}
            />
        </div>
    );
}
