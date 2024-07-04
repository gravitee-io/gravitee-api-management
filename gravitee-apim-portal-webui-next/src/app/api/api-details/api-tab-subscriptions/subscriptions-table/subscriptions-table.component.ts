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
import { Component, Input, OnInit } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatOption, MatSelect } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { catchError, map, Observable, startWith, switchMap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { LoaderComponent } from '../../../../../components/loader/loader.component';
import { Subscription, SubscriptionStatusEnum } from '../../../../../entities/subscription/subscription';
import { SubscriptionMetadata } from '../../../../../entities/subscription/subscriptions-response';
import { CapitalizeFirstPipe } from '../../../../../pipe/capitalize-first.pipe';
import { SubscriptionService } from '../../../../../services/subscription.service';

@Component({
  selector: 'app-subscriptions-table',
  standalone: true,
  imports: [
    AsyncPipe,
    MatTableModule,
    MatIcon,
    CapitalizeFirstPipe,
    MatFormField,
    MatLabel,
    MatSelect,
    ReactiveFormsModule,
    MatOption,
    RouterLink,
    LoaderComponent,
  ],
  providers: [CapitalizeFirstPipe],
  styleUrl: './subscriptions-table.component.scss',
  templateUrl: './subscriptions-table.component.html',
})
export class SubscriptionsTableComponent implements OnInit {
  @Input()
  apiId!: string;

  public displayedColumns: string[] = ['application', 'plan', 'status', 'expand'];
  public subscriptionStatusesList = Object.values(SubscriptionStatusEnum);
  public subscriptionsStatus = new FormControl();
  public subscriptionsList$!: Observable<Subscription[]>;

  constructor(
    private capitalizeFirstPipe: CapitalizeFirstPipe,
    private subscriptionService: SubscriptionService,
  ) {}

  ngOnInit(): void {
    this.subscriptionsList$ = this.loadSubscriptions$();
  }

  retrieveMetadataName(id: string, metadata: SubscriptionMetadata) {
    if (Object.hasOwn(metadata, id)) {
      return this.capitalizeFirstPipe.transform(<string>metadata[id]['name']);
    } else {
      return '-';
    }
  }

  private loadSubscriptions$(): Observable<Subscription[]> {
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
