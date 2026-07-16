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
import type { ReactNode } from 'react';

import type { DeveloperPortal } from '../../portals/types';
import styles from './AuthLayout.module.scss';

interface AuthLayoutProps {
    readonly portal: DeveloperPortal;
    readonly title: string;
    readonly subtitle?: string;
    readonly children: ReactNode;
}

export function AuthLayout({ portal, title, subtitle, children }: AuthLayoutProps) {
    const displayName = portal.portalLabel || portal.name;

    return (
        <div className={`portal-scope ${styles.authLayout}`}>
            <aside className={styles.brandPanel} aria-hidden="false">
                <div className={styles.brandContent}>
                    {portal.portalIconUrl ? (
                        <img src={portal.portalIconUrl} alt="" className={styles.portalIcon} />
                    ) : (
                        <div className={styles.portalIcon} aria-hidden="true" />
                    )}
                    <h1 className={styles.portalName}>{displayName}</h1>
                    <p className={styles.tagline}>
                        Build, explore, and integrate with APIs — your developer portal starts here.
                    </p>
                </div>
            </aside>

            <main className={styles.formPanel}>
                <div className={styles.formCard}>
                    <div className={styles.mobileBrand}>
                        {portal.portalIconUrl && (
                            <img src={portal.portalIconUrl} alt="" className={styles.mobileIcon} />
                        )}
                        <span className={styles.mobilePortalName}>{displayName}</span>
                    </div>
                    <h2 className={styles.formTitle}>{title}</h2>
                    {subtitle && <p className={styles.formSubtitle}>{subtitle}</p>}
                    {children}
                </div>
            </main>
        </div>
    );
}
