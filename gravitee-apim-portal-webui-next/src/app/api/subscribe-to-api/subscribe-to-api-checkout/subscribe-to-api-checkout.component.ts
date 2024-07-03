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
import { CdkCopyToClipboard } from '@angular/cdk/clipboard';
import { AsyncPipe } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { MatButtonModule, MatIconButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { catchError, Observable } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { Api } from '../../../../entities/api/api';
import { ToPeriodTimeUnitLabelPipe } from '../../../../pipe/time-unit.pipe';
import { ApiService } from '../../../../services/api.service';

@Component({
  selector: 'app-subscribe-to-api-checkout',
  imports: [
    MatCardModule,
    MatButtonModule,
    MatFormField,
    MatIcon,
    MatIconButton,
    MatLabel,
    MatInput,
    CdkCopyToClipboard,
    AsyncPipe,
    ToPeriodTimeUnitLabelPipe,
    MatSuffix,
  ],
  templateUrl: './subscribe-to-api-checkout.component.html',
  styleUrl: './subscribe-to-api-checkout.component.scss',
  providers: [ToPeriodTimeUnitLabelPipe],
  standalone: true,
})
export class SubscribeToApiCheckoutComponent implements OnInit {
  @Input()
  apiId!: string;

  @Input() subscribeForm!: FormGroup;

  subscriptionDetails: Observable<Api> = of();
  constructor(private apiService: ApiService) {}

  ngOnInit() {
    this.subscriptionDetails = this.loadDetails();
  }

  public formatCurlCommandLine(url: string): string {
    return `curl ${url}`;
  }

  goBackStepper() {
    this.subscribeForm.controls['step'].setValue(
      this.subscribeForm.value.step - (this.skipNextStep(this.subscribeForm.value.plan.security) ? 2 : 1),
    );
  }

  skipNextStep(authenticationType: string): boolean {
    return authenticationType === 'KEY_LESS';
  }

  private loadDetails(): Observable<Api> {
    return this.apiService.details(this.apiId).pipe(
      catchError(_ => {
        return of();
      }),
    );
  }
}
