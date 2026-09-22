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
import { useEffect } from 'react';

import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { useEnvHrid } from '../../environment/environment.utils';
import { redirectToPortalNextEditor } from '../portal-next';

/** Bookmarked `/environments/:envHrid/portals` opens the portal-next editor in the current tab. */
export function PortalNextEditorRedirect() {
    const envHrid = useEnvHrid();
    const consoleUrl = useBootstrapStore(s => s.config?.consoleUrl);

    useEffect(() => {
        if (!consoleUrl) {
            return;
        }
        redirectToPortalNextEditor(consoleUrl, envHrid);
    }, [consoleUrl, envHrid]);

    return null;
}
