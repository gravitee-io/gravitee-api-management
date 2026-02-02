/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import {Component, DestroyRef, effect, inject, input, resource, Signal, signal} from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import { LoadingValueComponent } from './loading-value.component';
import {PlanMode, PlanSecurityEnum, PlanUsageConfiguration} from "../../../entities/plan/plan";
import {
  Subscription,
  SubscriptionConsumerConfiguration,
  SubscriptionConsumerStatusEnum, SubscriptionsResponse,
  SubscriptionStatusEnum
} from "../../../entities/subscription";
import {Api, ApiType} from "../../../entities/api/api";
import {
  BehaviorSubject,
  catchError,
  defer,
  delay,
  firstValueFrom,
  forkJoin,
  map,
  Observable,
  switchMap,
  tap
} from "rxjs";
import {of} from "rxjs/internal/observable/of";
import {SubscriptionService} from "../../../services/subscription.service";
import {ApiService} from "../../../services/api.service";
import {ApplicationService} from "../../../services/application.service";
import {PermissionsService} from "../../../services/permissions.service";
import {PlanService} from "../../../services/plan.service";
import {MatDialog} from "@angular/material/dialog";
import {UserApiPermissions} from "../../../entities/permission/permission";
import {rxResource, toObservable, toSignal} from "@angular/core/rxjs-interop";
import {CopyButtonComponent} from "../../../components/copy-button/copy-button.component";
import {$localize} from "@angular/localize/init";

@Component({
  selector: 'app-subscription-details',
  imports: [RouterModule, LoadingValueComponent, CopyButtonComponent],
  templateUrl: './subscription-details.component.html',
  styleUrl: './subscription-details.component.scss',
  standalone: true,
})
export default class SubscriptionDetailsComponent {
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly applicationService = inject(ApplicationService);
  private readonly permissionsService = inject(PermissionsService);
  private readonly planService = inject(PlanService);
  private readonly apiService = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  protected readonly $localize = $localize;

  subscriptionId = input.required<string>();
  subscription = rxResource({
    params: () => this.subscriptionId(),
    stream: ({ params }) => this.subscriptionService.get(params).pipe(catchError(_ => {
      this.router.navigate(['404']);
      return of();
    }))
  });
  api = rxResource({
    params: () => this.subscription.value(),
    stream: ({ params }) => params ? this.apiService.details(params.api) : of(null)
  });
  application = rxResource({
    params: () => this.subscription.value(),
    stream: ({ params }) => params ? this.applicationService.get(params.application) : of(null)
  });
  plan = rxResource({
    params: () => this.subscription.value(),
    stream: ({ params }) => params ? this.permissionsService.getApiPermissions(params.api).pipe(
        switchMap((permissions) => this.getPlanData(params, permissions)),
        catchError(_ => of(null)),
      ) : of(null)
  });

  private getPlanData(
    subscription: Subscription,
    permissions: UserApiPermissions,
  ): Observable<{
    name: string;
  }> {
    const { plan: planId, api: apiId } = subscription;
    return of(permissions.PLAN?.includes('R') === true).pipe(
      switchMap(hasPermission => (hasPermission ? this.getPlanDataFromList(planId, apiId) : this.getPlanDataFromMetadata(planId, apiId))),
      map(plan => ({ name: plan.name ?? '' })),
    );
  }

  private getPlanDataFromList(planId: string, apiId: string): Observable<{
    name?: string;
  }> {
    return this.planService.list(apiId).pipe(
      map(({ data }) => {
        if (data && data.some(p => p.id === planId)) {
          const foundPlan = data.find(plan => plan.id === planId);
          return { name: foundPlan?.name };
        }
        return {};
      }),
      catchError(_ => this.getPlanDataFromMetadata(planId, apiId)),
    );
  }

  private getPlanDataFromMetadata(planId: string, apiId: string): Observable<{
    name?: string;
  }> {
    return this.subscriptionService.list({ apiId, statuses: [] }).pipe(
      catchError(_ => of({ data: [], metadata: {}, links: {} } as SubscriptionsResponse)),
      map(({ metadata }) => {
        const planMetadata = metadata[planId];
        return { name: planMetadata?.name };
      }),
    );
  }

  /**
   * TODO
   * mutualize with the table
   * create a pipe
   * use i18n
   */
  getStatusLabel(status?: string): string {
    const statusMap: Record<string, string> = {
      ACCEPTED: 'Active',
      PENDING_ACTIVATION: 'Pending activation',
      PAUSED: 'Suspended',
      CLOSED: 'Closed',
      PENDING: 'Pending',
      REJECTED: 'Rejected',
    };
    return status ? (statusMap[status] ?? status) : '';
  }
}
