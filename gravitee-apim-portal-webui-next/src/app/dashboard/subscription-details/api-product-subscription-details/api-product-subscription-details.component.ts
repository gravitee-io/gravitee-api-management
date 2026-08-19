/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { DatePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';

import { ApiProductSubscriptionApiAccessComponent } from './api-product-subscription-api-access/api-product-subscription-api-access.component';
import { ApiKeysListComponent } from '../../../../components/api-access/api-keys-list/api-keys-list.component';
import { CopyCodeComponent } from '../../../../components/copy-code/copy-code.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { getPlanSecurityTypeLabel } from '../../../../entities/plan/plan';
import { isActiveApiKey, Subscription } from '../../../../entities/subscription/subscription';
import { CapitalizeFirstPipe } from '../../../../pipe/capitalize-first.pipe';
import { ToPeriodTimeUnitLabelPipe } from '../../../../pipe/time-unit.pipe';
import { ApplicationService } from '../../../../services/application.service';

@Component({
  selector: 'app-api-product-subscription-details',
  imports: [
    ApiKeysListComponent,
    ApiProductSubscriptionApiAccessComponent,
    CapitalizeFirstPipe,
    CopyCodeComponent,
    DatePipe,
    LoaderComponent,
    MatCardModule,
    RouterLink,
    ToPeriodTimeUnitLabelPipe,
  ],
  templateUrl: './api-product-subscription-details.component.html',
  styleUrl: './api-product-subscription-details.component.scss',
})
export class ApiProductSubscriptionDetailsComponent {
  private readonly applicationService = inject(ApplicationService);

  readonly subscription = input.required<Subscription>();

  protected readonly product = computed(() => this.subscription().apiProduct);
  protected readonly plan = computed(() => this.product()?.plan);
  protected readonly planSecurityLabel = computed(() => getPlanSecurityTypeLabel(this.plan()?.security));
  protected readonly apiAccessEnabled = computed(() => {
    const status = this.subscription().status;
    return status === 'ACCEPTED' || (this.plan()?.security === 'KEY_LESS' && status !== 'CLOSED' && status !== 'REJECTED');
  });
  protected readonly activeApiKey = computed(() => this.subscription().keys?.find(apiKey => isActiveApiKey(apiKey))?.key);
  protected readonly applicationResource = rxResource({
    params: () => this.subscription().application,
    stream: ({ params }) => this.applicationService.get(params),
  });
  protected readonly clientId = computed(
    () => this.applicationResource.value()?.settings.oauth?.client_id ?? this.applicationResource.value()?.settings.app?.client_id,
  );
  protected readonly clientSecret = computed(() => this.applicationResource.value()?.settings.oauth?.client_secret);
}
