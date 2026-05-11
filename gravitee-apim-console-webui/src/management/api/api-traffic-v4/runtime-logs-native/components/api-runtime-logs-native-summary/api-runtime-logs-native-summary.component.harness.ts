/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { ComponentHarness } from '@angular/cdk/testing';

import { NativeConnectionStatus } from '../../../../../../entities/management-api-v2';

const STATUSES: NativeConnectionStatus[] = ['CONNECTED', 'SESSION_ERROR', 'CONNECTION_ERROR', 'INTERNAL_ERROR'];

export class ApiRuntimeLogsNativeSummaryHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-native-summary';

  async getCounts(): Promise<Record<NativeConnectionStatus, string>> {
    const entries = await Promise.all(
      STATUSES.map(async status => {
        const card = await this.locatorForOptional(`[data-testid="native_logs_summary_card_${status}"] .summary__card__count`)();
        return [status, card ? (await card.text()).trim() : ''] as const;
      }),
    );
    return Object.fromEntries(entries) as Record<NativeConnectionStatus, string>;
  }
}
