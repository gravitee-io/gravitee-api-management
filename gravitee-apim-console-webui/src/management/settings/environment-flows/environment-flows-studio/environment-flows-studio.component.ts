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
import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { GioIconsModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { BehaviorSubject, switchMap } from 'rxjs';
import {
  GioPolicyGroupStudioComponent,
  GioPolicyStudioComponent,
  PolicyDocumentationFetcher,
  PolicySchemaFetcher,
} from '@gravitee/ui-policy-studio-angular';
import { filter, map } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';

import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { PolicyV2Service } from '../../../../services-ngx/policy-v2.service';
import { EnvironmentFlowsService } from '../../../../services-ngx/environment-flows.service';
import { IconService } from '../../../../services-ngx/icon.service';
import {
  EnvironmentFlowsAddEditDialogComponent,
  EnvironmentFlowsAddEditDialogData,
  EnvironmentFlowsAddEditDialogResult,
} from '../environment-flows-add-edit-dialog/environment-flows-add-edit-dialog.component';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';

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
    GioPolicyStudioComponent,
    GioPermissionModule,
    GioPolicyGroupStudioComponent,
  ],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnvironmentFlowsStudioComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly environmentFlowsService = inject(EnvironmentFlowsService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly permissionService = inject(GioPermissionService);
  private readonly policyV2Service = inject(PolicyV2Service);
  private readonly iconService = inject(IconService);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private refresh$ = new BehaviorSubject<void>(undefined);

  protected isReadOnly = false;
  protected environmentFlow = toSignal(
    this.refresh$.pipe(switchMap(() => this.environmentFlowsService.get(this.activatedRoute.snapshot.params.environmentFlowId))),
  );
  protected policySchemaFetcher: PolicySchemaFetcher = (policy) => this.policyV2Service.getSchema(policy.id);
  protected policyDocumentationFetcher: PolicyDocumentationFetcher = (policy) => this.policyV2Service.getDocumentation(policy.id);
  protected policies$ = this.policyV2Service
    .list()
    .pipe(map((policies) => policies.map((policy) => ({ ...policy, icon: this.iconService.registerSvg(policy.id, policy.icon) }))));
  protected enableSaveButton = false;
  protected enableDeployButton = false;

  constructor() {
    this.isReadOnly = !this.permissionService.hasAnyMatching(['environment-environment_flows-r']);
  }

  public onEdit(): void {
    const environmentFlow = this.environmentFlow();
    this.matDialog
      .open<EnvironmentFlowsAddEditDialogComponent, EnvironmentFlowsAddEditDialogData, EnvironmentFlowsAddEditDialogResult>(
        EnvironmentFlowsAddEditDialogComponent,
        {
          data: { environmentFlow },
          role: 'dialog',
          id: 'test-story-dialog',
        },
      )
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((payload) =>
          this.environmentFlowsService.update(environmentFlow.id, {
            name: payload.name,
            description: payload.description,
          }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Environment flow updated');
          this.refresh$.next();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error during environment flow update!');
        },
      });
  }

  public onDelete(): void {
    // TODO
  }

  public onDeploy(): void {
    // TODO
    this.enableDeployButton = false;
  }

  public onSave(): void {
    const environmentFlow = this.environmentFlow();
    this.enableSaveButton = false;

    this.environmentFlowsService
      .update(environmentFlow.id, {
        name: environmentFlow.name,
        description: environmentFlow.description,
        policies: environmentFlow.policies,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackBarService.success('Environment flow updated');
          this.refresh$.next();
          this.enableDeployButton = true;
        },
        error: (error) => {
          this.enableSaveButton = true;
          this.snackBarService.error(error?.error?.message ?? 'Error during environment flow update!');
        },
      });
  }

  public onStudioChange(steps: any): void {
    this.environmentFlow().policies = steps;
    this.enableSaveButton = true;
  }
}
