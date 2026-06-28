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
import { Popover, PopoverContent, PopoverTrigger, Tooltip, TooltipContent, TooltipTrigger } from '@gravitee/graphene-core';

import type { BlockStyleOverrides } from '../types/block-styles.types';
import { ColorInput } from './ColorInput';
import styles from './StylePropertiesStrip.module.scss';

interface StylePropertiesStripProps {
    readonly overrides: BlockStyleOverrides;
    readonly onChange: (overrides: BlockStyleOverrides) => void;
    readonly onReset: () => void;
    readonly hasOverrides: boolean;
    readonly label?: string;
}

export function StylePropertiesStrip({
    overrides,
    onChange,
    onReset,
    hasOverrides,
    label = 'Block styles',
}: StylePropertiesStripProps) {
    return (
        <div className={styles.strip} aria-label={label}>
            <Popover>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <PopoverTrigger asChild>
                            <button type="button" className={styles.iconButton} aria-label="Text color">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                                    <path d="M4 20h16" />
                                    <path d="M9.5 4h5l4 12H5.5z" />
                                </svg>
                                {overrides.color && (
                                    <span className={styles.colorDot} style={{ background: overrides.color }} />
                                )}
                            </button>
                        </PopoverTrigger>
                    </TooltipTrigger>
                    <TooltipContent>Text color</TooltipContent>
                </Tooltip>
                <PopoverContent className={styles.popover} align="start" sideOffset={8}>
                    <label className={styles.popoverLabel}>Text Color</label>
                    <ColorInput
                        value={overrides.color ?? ''}
                        onChange={color => onChange({ ...overrides, color })}
                    />
                </PopoverContent>
            </Popover>

            <Popover>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <PopoverTrigger asChild>
                            <button type="button" className={styles.iconButton} aria-label="Background color">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                                    <rect x="3" y="3" width="18" height="18" rx="2" fill={overrides.backgroundColor || 'none'} />
                                </svg>
                            </button>
                        </PopoverTrigger>
                    </TooltipTrigger>
                    <TooltipContent>Background</TooltipContent>
                </Tooltip>
                <PopoverContent className={styles.popover} align="start" sideOffset={8}>
                    <label className={styles.popoverLabel}>Background</label>
                    <ColorInput
                        value={overrides.backgroundColor ?? ''}
                        onChange={backgroundColor => onChange({ ...overrides, backgroundColor })}
                    />
                </PopoverContent>
            </Popover>

            <Popover>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <PopoverTrigger asChild>
                            <button type="button" className={styles.iconButton} aria-label="Font size">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                                    <path d="M4 7V4h16v3" />
                                    <path d="M12 4v16" />
                                    <path d="M8 20h8" />
                                </svg>
                            </button>
                        </PopoverTrigger>
                    </TooltipTrigger>
                    <TooltipContent>Font size</TooltipContent>
                </Tooltip>
                <PopoverContent className={styles.popover} align="start" sideOffset={8}>
                    <label className={styles.popoverLabel}>Font Size</label>
                    <input
                        type="text"
                        className={styles.valueInput}
                        value={overrides.fontSize ?? ''}
                        placeholder="inherit"
                        onChange={e => onChange({ ...overrides, fontSize: e.target.value || undefined })}
                    />
                </PopoverContent>
            </Popover>

            <Popover>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <PopoverTrigger asChild>
                            <button type="button" className={styles.iconButton} aria-label="Border radius">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M3 12V5a2 2 0 0 1 2-2h7" />
                                    <path d="M12 3h7a2 2 0 0 1 2 2v7" />
                                    <path d="M21 12v7a2 2 0 0 1-2 2h-7" />
                                    <path d="M12 21H5a2 2 0 0 1-2-2v-7" />
                                </svg>
                            </button>
                        </PopoverTrigger>
                    </TooltipTrigger>
                    <TooltipContent>Border radius</TooltipContent>
                </Tooltip>
                <PopoverContent className={styles.popover} align="start" sideOffset={8}>
                    <label className={styles.popoverLabel}>Border Radius</label>
                    <input
                        type="text"
                        className={styles.valueInput}
                        value={overrides.borderRadius ?? ''}
                        placeholder="inherit"
                        onChange={e => onChange({ ...overrides, borderRadius: e.target.value || undefined })}
                    />
                </PopoverContent>
            </Popover>

            <Popover>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <PopoverTrigger asChild>
                            <button type="button" className={styles.iconButton} aria-label="Padding">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <rect x="3" y="3" width="18" height="18" rx="2" />
                                    <rect x="7" y="7" width="10" height="10" rx="1" />
                                </svg>
                            </button>
                        </PopoverTrigger>
                    </TooltipTrigger>
                    <TooltipContent>Padding</TooltipContent>
                </Tooltip>
                <PopoverContent className={styles.popover} align="start" sideOffset={8}>
                    <label className={styles.popoverLabel}>Padding</label>
                    <input
                        type="text"
                        className={styles.valueInput}
                        value={overrides.padding ?? ''}
                        placeholder="inherit"
                        onChange={e => onChange({ ...overrides, padding: e.target.value || undefined })}
                    />
                </PopoverContent>
            </Popover>

            {hasOverrides && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <button type="button" className={styles.resetButton} onClick={onReset} aria-label="Reset to theme defaults">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                                <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8" />
                                <path d="M21 3v5h-5" />
                                <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16" />
                                <path d="M3 21v-5h5" />
                            </svg>
                        </button>
                    </TooltipTrigger>
                    <TooltipContent>Reset to defaults</TooltipContent>
                </Tooltip>
            )}
        </div>
    );
}
