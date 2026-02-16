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
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

import { Scope, Tuple } from '../../../../../entities/alert';
import { TenantService } from '../../../../../services-ngx/tenant.service';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { PlanService } from '../../../../../services-ngx/plan.service';
import { SubscriptionService } from '../../../../../services-ngx/subscription.service';
import { gatewayErrorKeys } from '../../../../../entities/gateway-error-keys/GatewayErrorKeys';
import { statusLoader } from '../../../../../entities/alerts/healthcheck.metrics';
import { EndpointGroupV2, EndpointGroupV4, EndpointV4, HttpEndpointV2 } from '../../../../../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class RuntimeAlertCreateService {
  constructor(
    private readonly tenantService: TenantService,
    private readonly apiService: ApiV2Service,
    private readonly planService: PlanService,
    private readonly subscriptionService: SubscriptionService,
  ) {}

  public loadDataFromMetric(key: string, referenceType: Scope, referenceId: string): Observable<Tuple[]> {
    switch (key) {
      case 'tenant':
        return this.loadTenants();
      case 'application':
        return this.loadApplications(referenceId);
      case 'plan':
        return this.loadPlans(referenceType, referenceId);
      case 'error.key':
        return this.loadErrorKeys();
      case 'status.old':
      case 'status.new':
        return this.loadHealthCheckStatus();
      case 'endpoint.name':
        return this.loadEndpointsNames(referenceId);
      default:
        return null;
    }
  }

  private loadPlans(referenceType: Scope, referenceId: string): Observable<Tuple[]> {
    if (Scope.API === referenceType) {
      return this.planService
        .getApiPlans(referenceId, 'PUBLISHED')
        .pipe(map(plans => plans?.map(subscribers => new Tuple(subscribers.id, subscribers.name))));
    }

    if (Scope.APPLICATION === referenceType) {
      return this.subscriptionService.getApplicationSubscriptions(referenceId).pipe(
        map(subscribers => {
          const keyPlans = [...new Set(subscribers.data.map(subscriber => subscriber.plan))];
          return keyPlans.map(plan => new Tuple(plan, subscribers.metadata[plan].name));
        }),
      );
    }

    return null;
  }

  private loadApplications(apiId: string): Observable<Tuple[]> {
    return this.apiService
      .getSubscribers(apiId)
      .pipe(map(subscribers => subscribers.data?.map(subscribers => new Tuple(subscribers.id, subscribers.name))));
  }

  private loadTenants(): Observable<Tuple[]> {
    return this.tenantService.list().pipe(map(tenants => tenants.map(tenant => new Tuple(tenant.id, tenant.name))));
  }

  private loadErrorKeys() {
    return of(gatewayErrorKeys.map(key => new Tuple(key, key)));
  }

  private loadHealthCheckStatus() {
    return of(statusLoader());
  }

  private loadEndpointsNames(apiId: string) {
    return this.apiService.get(apiId).pipe(
      map(api => {
        if (api.definitionVersion === 'FEDERATED' || api.definitionVersion === 'FEDERATED_AGENT') {
          return [];
        }
        return api.definitionVersion === 'V4' ? this.mapGroups(api.endpointGroups) : this.mapGroups(api.proxy.groups);
      }),
    );
  }

  private mapGroups(groups: (EndpointGroupV4 | EndpointGroupV2)[]) {
    return groups.reduce((acc: Tuple[], group: EndpointGroupV4 | EndpointGroupV2) => this.groupToTuples(acc, group), []);
  }

  private groupToTuples(acc: Tuple[], group: EndpointGroupV4 | EndpointGroupV2) {
    acc.push(...group.endpoints.map((endpoint: EndpointV4 | HttpEndpointV2) => new Tuple(endpoint.name, endpoint.name)));
    return acc;
  }
}
