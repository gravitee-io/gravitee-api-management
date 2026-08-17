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

import { useMutation, useQueryClient } from '@tanstack/react-query';

import { notify } from '../../../shared/notify';
import type { ConsoleSettings } from '../types/consoleSettings';
import { saveOrgConsoleSettings } from '../services/consoleSettings';
import { orgConsoleSettingsKeys } from '../utils/queryKeys';

export function useSaveOrgConsoleSettings() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (payload: ConsoleSettings) => saveOrgConsoleSettings(payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orgConsoleSettingsKeys.all });
            notify.success('Configuration successfully saved!');
        },
        onError: error => notify.error(error, 'An error occurred while saving the configuration.'),
    });
}
