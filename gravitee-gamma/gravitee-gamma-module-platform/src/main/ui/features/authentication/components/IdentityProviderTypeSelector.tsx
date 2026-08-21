/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { cn } from '@gravitee/graphene-core';
import { LockIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useRef, useState, type KeyboardEvent } from 'react';

import { IdentityProviderTypeIcon } from './IdentityProviderTypeIcon';
import { OpenIdConnectSsoLicenseDialog } from './OpenIdConnectSsoLicenseDialog';
import type { IdentityProviderType } from '../types/identityProvider';
import { IDENTITY_PROVIDER_TYPES } from '../utils/identityProviderDisplay';

function nextTypeIndex(currentIndex: number, key: string, typeCount: number): number {
    if (key === 'Home') return 0;
    if (key === 'End') return typeCount - 1;
    if (key === 'ArrowRight' || key === 'ArrowDown') return (currentIndex + 1) % typeCount;
    if (key === 'ArrowLeft' || key === 'ArrowUp') return (currentIndex - 1 + typeCount) % typeCount;
    return currentIndex;
}

export function IdentityProviderTypeSelector({
    value,
    hasOpenIdConnectLicense,
    onChange,
}: Readonly<{
    value: IdentityProviderType;
    hasOpenIdConnectLicense: boolean;
    onChange: (type: IdentityProviderType) => void;
}>) {
    const [licenseOpen, setLicenseOpen] = useState(false);
    const [focusedType, setFocusedType] = useState<IdentityProviderType>(value);
    const buttonRefs = useRef<Partial<Record<IdentityProviderType, HTMLButtonElement | null>>>({});
    const ignoreClickRef = useRef(false);

    useEffect(() => {
        setFocusedType(value);
    }, [value]);

    function isSelectable(type: IdentityProviderType): boolean {
        return type !== 'OIDC' || hasOpenIdConnectLicense;
    }

    function activateType(type: IdentityProviderType) {
        if (!isSelectable(type)) {
            setLicenseOpen(true);
            return;
        }
        if (type === value) {
            return;
        }
        onChange(type);
    }

    function typeForButton(button: HTMLButtonElement): IdentityProviderType | undefined {
        return IDENTITY_PROVIDER_TYPES.find(type => buttonRefs.current[type.value] === button)?.value;
    }

    function handleTypeKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
        if (event.key === ' ' || event.key === 'Enter') {
            event.preventDefault();
            const type = typeForButton(event.currentTarget);
            if (type) {
                activateType(type);
            }
            return;
        }
        if (!['ArrowRight', 'ArrowDown', 'ArrowLeft', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
            return;
        }
        event.preventDefault();
        const currentType = typeForButton(event.currentTarget) ?? focusedType;
        const currentIndex = Math.max(
            0,
            IDENTITY_PROVIDER_TYPES.findIndex(type => type.value === currentType),
        );
        const next = IDENTITY_PROVIDER_TYPES[nextTypeIndex(currentIndex, event.key, IDENTITY_PROVIDER_TYPES.length)];
        if (!next) return;
        ignoreClickRef.current = true;
        setFocusedType(next.value);
        buttonRefs.current[next.value]?.focus();
        window.setTimeout(() => {
            ignoreClickRef.current = false;
        }, 0);
    }

    return (
        <>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2" role="radiogroup" aria-label="Provider type">
                {IDENTITY_PROVIDER_TYPES.map(type => {
                    const isSelected = value === type.value;
                    const isLocked = type.value === 'OIDC' && !hasOpenIdConnectLicense;

                    return (
                        <button
                            key={type.value}
                            ref={element => {
                                buttonRefs.current[type.value] = element;
                            }}
                            type="button"
                            role="radio"
                            tabIndex={focusedType === type.value ? 0 : -1}
                            aria-checked={isSelected}
                            aria-label={isLocked ? `${type.label}, requires an enterprise license` : undefined}
                            className={cn(
                                'relative flex items-center gap-3 rounded-xl border p-4 text-left outline-none transition-all focus-visible:ring-2 focus-visible:ring-primary/40',
                                isSelected ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/40',
                                isLocked && 'opacity-80',
                            )}
                            onKeyDown={handleTypeKeyDown}
                            onClick={() => {
                                setFocusedType(type.value);
                                if (ignoreClickRef.current) {
                                    return;
                                }
                                activateType(type.value);
                            }}
                        >
                            <span
                                className={cn(
                                    'flex size-4 shrink-0 items-center justify-center rounded-full border',
                                    isSelected ? 'border-primary' : 'border-muted-foreground',
                                )}
                                aria-hidden
                            >
                                {isSelected ? <span className="size-2 rounded-full bg-primary" /> : null}
                            </span>
                            <IdentityProviderTypeIcon type={type.value} className="size-8 shrink-0" />
                            <span className="text-sm font-medium">{type.label}</span>
                            {isLocked ? <LockIcon className="absolute top-3 right-3 size-4 text-muted-foreground" aria-hidden /> : null}
                        </button>
                    );
                })}
            </div>
            <OpenIdConnectSsoLicenseDialog
                open={licenseOpen}
                onOpenChange={open => {
                    setLicenseOpen(open);
                    if (!open) {
                        setFocusedType(value);
                    }
                }}
            />
        </>
    );
}
