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

import type { ApiLoggingFormState } from './apiLoggingValidators';
import type { ConsoleSettings } from '../../organization-settings/types/consoleSettings';
import { isConsoleSettingReadonly } from '../../organization-settings/utils/isConsoleSettingReadonly';

export interface ApiLoggingFieldReadonly {
    maxDurationMillis: boolean;
    auditEnabled: boolean;
    auditTrailEnabled: boolean;
    userDisplayed: boolean;
    probabilisticDefault: boolean;
    probabilisticLimit: boolean;
    countDefault: boolean;
    countLimit: boolean;
    temporalDefault: boolean;
    temporalLimit: boolean;
    windowedCountDefault: boolean;
    windowedCountLimit: boolean;
}

export function buildApiLoggingFormState(settings: ConsoleSettings | undefined): ApiLoggingFormState {
    const logging = settings?.logging;
    const sampling = logging?.messageSampling;

    return {
        maxDurationMillis: String(logging?.maxDurationMillis ?? 0),
        auditEnabled: logging?.audit?.enabled ?? false,
        auditTrailEnabled: logging?.audit?.trail?.enabled ?? false,
        userDisplayed: logging?.user?.displayed ?? false,
        probabilisticDefault: String(sampling?.probabilistic?.default ?? ''),
        probabilisticLimit: String(sampling?.probabilistic?.limit ?? ''),
        countDefault: String(sampling?.count?.default ?? ''),
        countLimit: String(sampling?.count?.limit ?? ''),
        temporalDefault: sampling?.temporal?.default ?? '',
        temporalLimit: sampling?.temporal?.limit ?? '',
        windowedCountDefault: sampling?.windowedCount?.default ?? '',
        windowedCountLimit: sampling?.windowedCount?.limit ?? '',
    };
}

export function getApiLoggingReadonlyState(settings: ConsoleSettings | undefined): ApiLoggingFieldReadonly {
    return {
        maxDurationMillis: isConsoleSettingReadonly(settings, 'logging.default.max.duration'),
        auditEnabled: isConsoleSettingReadonly(settings, 'logging.audit.enabled'),
        auditTrailEnabled: isConsoleSettingReadonly(settings, 'logging.audit.trail.enabled'),
        userDisplayed: isConsoleSettingReadonly(settings, 'logging.user.displayed'),
        probabilisticDefault: isConsoleSettingReadonly(settings, 'logging.messageSampling.probabilistic.default'),
        probabilisticLimit: isConsoleSettingReadonly(settings, 'logging.messageSampling.probabilistic.limit'),
        countDefault: isConsoleSettingReadonly(settings, 'logging.messageSampling.count.default'),
        countLimit: isConsoleSettingReadonly(settings, 'logging.messageSampling.count.limit'),
        temporalDefault: isConsoleSettingReadonly(settings, 'logging.messageSampling.temporal.default'),
        temporalLimit: isConsoleSettingReadonly(settings, 'logging.messageSampling.temporal.limit'),
        windowedCountDefault: isConsoleSettingReadonly(settings, 'logging.messageSampling.windowedCount.default'),
        windowedCountLimit: isConsoleSettingReadonly(settings, 'logging.messageSampling.windowedCount.limit'),
    };
}
