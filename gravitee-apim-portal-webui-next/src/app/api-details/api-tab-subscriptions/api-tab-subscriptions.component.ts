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
import { AsyncPipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatOption, MatSelect } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { catchError, map, Observable, startWith, switchMap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { SubscriptionMetadata, SubscriptionData, SubscriptionStatusEnum } from '../../../entities/subscription/subscription';
import { CapitalizeFirstPipe } from '../../../pipe/capitalize-first.pipe';
import { SubscriptionService } from '../../../services/subscription.service';

@Component({
  selector: 'app-api-tab-subscriptions',
  standalone: true,
  providers: [CapitalizeFirstPipe],
  imports: [AsyncPipe, MatTableModule, MatIcon, CapitalizeFirstPipe, MatFormField, MatLabel, MatSelect, ReactiveFormsModule, MatOption],
  templateUrl: './api-tab-subscriptions.component.html',
  styleUrl: './api-tab-subscriptions.component.scss',
})
export class ApiTabSubscriptionsComponent {
  @Input()
  apiId!: string;

  public displayedColumns: string[] = ['application', 'plan', 'status', 'expand'];
  public subscriptionStatusesList = Object.values(SubscriptionStatusEnum);
  public subscriptionsStatus = new FormControl();
  public subscriptionsList$!: Observable<SubscriptionData[]>;

  constructor(
    private capitalizeFirstPipe: CapitalizeFirstPipe,
    private subscriptionService: SubscriptionService,
  ) {
    this.subscriptionsList$ = this.loadSubscriptions$();
  }

  retrieveMetadataName(id: string, metadata: SubscriptionMetadata) {
    if (Object.hasOwn(metadata, id)) {
      return this.capitalizeFirstPipe.transform(<string>metadata[id]['name']);
    } else {
      return '-';
    }
  }

  private loadSubscriptions$(): Observable<SubscriptionData[]> {
    return this.subscriptionsStatus.valueChanges.pipe(
      map(status => status.map((value: string) => value.toUpperCase())),
      startWith(this.subscriptionsStatus.value),
      switchMap(status => this.subscriptionService.list(this.apiId, status)),
      map(response => {
        return response.data
          ? response.data.map(sub => ({
              ...sub,
              plan: this.retrieveMetadataName(<string>sub.plan, response.metadata),
              application: this.retrieveMetadataName(<string>sub.application, response.metadata),
            }))
          : [];
      }),
      catchError(_ => of([])),
    );
  }
}
