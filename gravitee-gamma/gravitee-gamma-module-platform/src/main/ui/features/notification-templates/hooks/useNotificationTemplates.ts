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

import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

import { useConsoleSettings } from '../../../shared/console-settings';
import { listNotificationTemplates } from '../services/notificationTemplates';
import { notificationTemplateKeys } from '../utils/queryKeys';
import { groupTemplatesByCategory } from '../utils/templateDisplay';

export function useNotificationTemplates() {
    const consoleSettings = useConsoleSettings();
    const alertEnabled = consoleSettings?.alert?.enabled === true;

    const listQuery = useQuery({
        queryKey: notificationTemplateKeys.list(),
        queryFn: listNotificationTemplates,
    });

    const categories = useMemo(() => groupTemplatesByCategory(listQuery.data ?? [], { alertEnabled }), [listQuery.data, alertEnabled]);
    const { templateCount, customCount } = useMemo(
        () => ({
            templateCount: categories.reduce((sum, category) => sum + category.rows.length, 0),
            customCount: categories.reduce((sum, category) => sum + category.customCount, 0),
        }),
        [categories],
    );

    return {
        categories,
        templateCount,
        customCount,
        isLoading: listQuery.isLoading,
        isError: listQuery.isError,
        error: listQuery.error,
        refetch: listQuery.refetch,
    };
}
