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
import { Observable } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

import { SubscriptionApiKeysComponent } from '../components/subscription-api-keys/subscription-api-keys.component';
import { Application } from '../../../../../entities/application/Application';
import { ApplicationService } from '../../../../../services-ngx/application.service';

@Component({
  selector: 'application-shared-api-keys',
  templateUrl: './application-shared-api-keys.component.html',
  styleUrls: ['./application-shared-api-keys.component.scss'],
  imports: [CommonModule, SubscriptionApiKeysComponent],
  standalone: true,
})
export class ApplicationSharedApiKeysComponent {
  private readonly applicationService = inject(ApplicationService);
  private readonly activatedRoute = inject(ActivatedRoute);

  public application$: Observable<Application> = this.applicationService.getLastApplicationFetch(
    this.activatedRoute.snapshot.params.applicationId,
  );
}
