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
import { ContextSidebar, ContextToggleButton, useLayoutConfig } from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';

import { CloudSettingsSidebarNav } from './CloudSettingsSidebarNav';
import { CLOUD_SETTINGS_NAV_LABELS, resolveCloudSettingsNavKey } from '../cloud-settings-navigation';
import { useEnvHrid } from '../../../features/environment/environment.utils';

export function CloudSettingsLayout() {
    const envHrid = useEnvHrid();
    const { pathname } = useLocation();
    const [contextExpanded, setContextExpanded] = useState(true);

    const basePath = `/environments/${envHrid}/cloud/settings`;
    const activeKey = useMemo(() => resolveCloudSettingsNavKey(pathname, envHrid), [pathname, envHrid]);
    const activeLabel = CLOUD_SETTINGS_NAV_LABELS[activeKey];

    useLayoutConfig(
        {
            viewMode: 'context',
            contextExpanded,
            contextSidebar: (
                <ContextSidebar>
                    <CloudSettingsSidebarNav basePath={basePath} />
                </ContextSidebar>
            ),
            leading: <ContextToggleButton expanded={contextExpanded} onToggle={() => setContextExpanded(v => !v)} />,
        },
        [contextExpanded, basePath],
    );

    return <Outlet context={{ activeLabel }} />;
}
