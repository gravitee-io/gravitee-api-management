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
import { Component, DestroyRef, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { GioIconsModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import {
  GioEnvironmentFlowStudioComponent,
  GioPolicyStudioComponent,
  PolicyDocumentationFetcher,
  PolicySchemaFetcher,
} from '@gravitee/ui-policy-studio-angular';
import { map } from 'rxjs/operators';

import { EnvironmentFlow } from '../../../../entities/management-api-v2';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { PolicyV2Service } from '../../../../services-ngx/policy-v2.service';
import { EnvironmentFlowsService } from '../../../../services-ngx/environment-flows.service';
import { IconService } from '../../../../services-ngx/icon.service';

@Component({
  selector: 'environment-flows-studio',
  templateUrl: './environment-flows-studio.component.html',
  styleUrls: ['./environment-flows-studio.component.scss'],
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    GioIconsModule,
    RouterLink,
    GioLoaderModule,
    GioEnvironmentFlowStudioComponent,
    GioPolicyStudioComponent,
  ],
  standalone: true,
})
export class EnvironmentFlowsStudioComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly environmentFlowsService = inject(EnvironmentFlowsService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly permissionService = inject(GioPermissionService);
  private readonly policyV2Service = inject(PolicyV2Service);
  private readonly iconService = inject(IconService);

  protected isReadOnly = false;
  protected environmentFlow$: Observable<EnvironmentFlow>;
  protected policySchemaFetcher: PolicySchemaFetcher = (policy) => this.policyV2Service.getSchema(policy.id);
  protected policyDocumentationFetcher: PolicyDocumentationFetcher = (policy) => this.policyV2Service.getDocumentation(policy.id);
  protected policies$ = this.policyV2Service
    .list()
    .pipe(map((policies) => policies.map((policy) => ({ ...policy, icon: this.iconService.registerSvg(policy.id, policy.icon) }))));

  constructor() {
    this.isReadOnly = !this.permissionService.hasAnyMatching(['environment-environment_flows-r']);
    const environmentFlowId = this.activatedRoute.snapshot.params['environmentFlowId'];

    this.environmentFlow$ = this.environmentFlowsService.get(environmentFlowId).pipe(takeUntilDestroyed(this.destroyRef));
  }

  public onEdit(): void {
    // TODO
  }

  public onDelete(): void {
    // TODO
  }

  public onDeploy(): void {
    // TODO
  }

  public onSave(): void {}
}
