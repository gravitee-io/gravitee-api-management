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
import { Component, inject, Input, OnInit } from '@angular/core';
import { catchError, map, Observable } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { SubscriptionData } from '../../../entities/subscription/subscription';
import { SubscriptionService } from '../../../services/subscription.service';

@Component({
  selector: 'app-api-tab-subscriptions',
  standalone: true,
  imports: [AsyncPipe],
  templateUrl: './api-tab-subscriptions.component.html',
  styleUrl: './api-tab-subscriptions.component.scss',
})
export class ApiTabSubscriptionsComponent implements OnInit {
  @Input()
  apiId!: string;

  subscriptionsList$!: Observable<SubscriptionData[]>;
  private subscriptionService = inject(SubscriptionService);

  constructor() {}

  ngOnInit(): void {
    this.subscriptionsList$ = this.loadSubscriptions$();
  }

  private loadSubscriptions$(): Observable<SubscriptionData[]> {
    return this.subscriptionService.list(this.apiId).pipe(
      map(response => {
        return response.data ?? [];
      }),
      catchError(_ => of([])),
    );
  }
}
