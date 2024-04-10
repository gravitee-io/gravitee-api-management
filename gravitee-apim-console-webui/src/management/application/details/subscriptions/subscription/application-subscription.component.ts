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
import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute } from '@angular/router';
import { GioClipboardModule, GioIconsModule, GioLoaderModule } from '@gravitee/ui-particles-angular';

import { ApplicationService } from '../../../../../services-ngx/application.service';

@Component({
  selector: 'application-subscription',
  templateUrl: './application-subscription.component.html',
  styleUrls: ['./application-subscription.component.scss'],
  imports: [CommonModule, MatCardModule, MatButtonModule, GioLoaderModule, GioIconsModule, GioClipboardModule],
  standalone: true,
})
export class ApplicationSubscriptionComponent {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly applicationService = inject(ApplicationService);

  public subscription$ = this.applicationService.getSubscription(
    this.activatedRoute.snapshot.params.applicationId,
    this.activatedRoute.snapshot.params.subscriptionId,
  );
}
