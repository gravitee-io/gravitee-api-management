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
import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { NATIVE_STATUS_META, NATIVE_SUMMARY_STATUSES } from '../../api-runtime-logs-native.models';
import { ApiNativeLogsV2Service } from '../../../../../../services-ngx/api-native-logs-v2.service';
import { NativeConnectionStatus } from '../../../../../../entities/management-api-v2';

@Component({
  selector: 'api-runtime-logs-native-summary',
  templateUrl: './api-runtime-logs-native-summary.component.html',
  styleUrls: ['./api-runtime-logs-native-summary.component.scss'],
  standalone: true,
  imports: [MatButtonModule, MatCardModule, MatIconModule, GioIconsModule],
})
export class ApiRuntimeLogsNativeSummaryComponent {
  private readonly logsService = inject(ApiNativeLogsV2Service);

  apiId = input.required<string>();
  from = input<number | null>(null);
  to = input<number | null>(null);

  protected readonly cards = NATIVE_SUMMARY_STATUSES.map(status => ({ status, ...NATIVE_STATUS_META[status] }));

  protected readonly summary = rxResource({
    params: () => {
      const from = this.from();
      const to = this.to();
      if (from == null || to == null) return undefined;
      return { apiId: this.apiId(), from, to };
    },
    stream: ({ params }) => this.logsService.searchSummary(params.apiId, params.from, params.to),
  });

  protected readonly counts = computed<Partial<Record<NativeConnectionStatus, number>>>(
    () => this.summary.value()?.countByConnectionStatus ?? {},
  );

  // Forces a re-fetch with the current params — used by the parent's refresh button.
  reload(): void {
    this.summary.reload();
  }
}
