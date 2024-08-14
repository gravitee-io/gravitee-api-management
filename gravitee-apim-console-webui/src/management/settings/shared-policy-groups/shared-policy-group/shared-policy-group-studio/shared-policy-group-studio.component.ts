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
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
import { MatTooltip } from '@angular/material/tooltip';

import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { PolicyV2Service } from '../../../../../services-ngx/policy-v2.service';
import { SharedPolicyGroupsService } from '../../../../../services-ngx/shared-policy-groups.service';
import { IconService } from '../../../../../services-ngx/icon.service';
import {
  SharedPolicyGroupsAddEditDialogComponent,
  SharedPolicyGroupAddEditDialogData,
  SharedPolicyGroupAddEditDialogResult,
} from '../../shared-policy-groups-add-edit-dialog/shared-policy-groups-add-edit-dialog.component';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { removeSharedPolicyGroup } from '../../shared-policy-groups.component';
import { SharedPolicyGroupsStateBadgeComponent } from '../../shared-policy-groups-state-badge/shared-policy-groups-state-badge.component';
import { toReadableExecutionPhase } from '../../../../../entities/management-api-v2';

@Component({
  selector: 'shared-policy-group-studio',
  templateUrl: './shared-policy-group-studio.component.html',
  styleUrls: ['./shared-policy-group-studio.component.scss'],
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
    MatTooltip,
    SharedPolicyGroupsStateBadgeComponent,
  ],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharedPolicyGroupStudioComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly sharedPolicyGroupsService = inject(SharedPolicyGroupsService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly permissionService = inject(GioPermissionService);
  private readonly policyV2Service = inject(PolicyV2Service);
  private readonly iconService = inject(IconService);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private refresh$ = new BehaviorSubject<void>(undefined);

  protected isReadOnly = !this.permissionService.hasAnyMatching(['environment-shared_policy_group-u']);
  protected sharedPolicyGroup = toSignal(
    this.refresh$.pipe(switchMap(() => this.sharedPolicyGroupsService.get(this.activatedRoute.snapshot.params.sharedPolicyGroupId))),
  );
  protected policySchemaFetcher: PolicySchemaFetcher = (policy) => this.policyV2Service.getSchema(policy.id);
  protected policyDocumentationFetcher: PolicyDocumentationFetcher = (policy) => this.policyV2Service.getDocumentation(policy.id);
  protected policies$ = this.policyV2Service
    .list()
    .pipe(map((policies) => policies.map((policy) => ({ ...policy, icon: this.iconService.registerSvg(policy.id, policy.icon) }))));
  protected enableSaveButton = false;
  protected toReadableExecutionPhase = toReadableExecutionPhase;

  public onEdit(): void {
    const sharedPolicyGroup = this.sharedPolicyGroup();
    this.matDialog
      .open<SharedPolicyGroupsAddEditDialogComponent, SharedPolicyGroupAddEditDialogData, SharedPolicyGroupAddEditDialogResult>(
        SharedPolicyGroupsAddEditDialogComponent,
        {
          data: { sharedPolicyGroup },
          role: 'dialog',
          id: 'test-story-dialog',
        },
      )
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((payload) =>
          this.sharedPolicyGroupsService.update(sharedPolicyGroup.id, {
            name: payload.name,
            description: payload.description,
            prerequisiteMessage: payload.prerequisiteMessage,
          }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Shared Policy Group updated');
          this.refresh$.next();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error during Shared Policy Group update!');
        },
      });
  }

  public onDeploy(): void {
    this.sharedPolicyGroupsService
      .deploy(this.sharedPolicyGroup().id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackBarService.success('Shared Policy Group deployed successfully');
          this.refresh$.next();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error during Shared Policy Group deployment!');
        },
      });
  }

  public onUndeploy(): void {
    this.sharedPolicyGroupsService
      .undeploy(this.sharedPolicyGroup().id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackBarService.success('Shared Policy Group undeployed successfully');
          this.refresh$.next();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error during Shared Policy Group undeployment!');
        },
      });
  }

  public onSave(): void {
    const sharedPolicyGroup = this.sharedPolicyGroup();
    this.enableSaveButton = false;

    this.sharedPolicyGroupsService
      .update(sharedPolicyGroup.id, {
        name: sharedPolicyGroup.name,
        description: sharedPolicyGroup.description,
        steps: sharedPolicyGroup.steps,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackBarService.success('Shared Policy Group updated');
          this.refresh$.next();
        },
        error: (error) => {
          this.enableSaveButton = true;
          this.snackBarService.error(error?.error?.message ?? 'Error during Shared Policy Group update!');
        },
      });
  }

  public onStudioChange(steps: any): void {
    this.sharedPolicyGroup().steps = steps;
    this.enableSaveButton = true;
  }

  protected onDelete() {
    removeSharedPolicyGroup(
      this.matDialog,
      this.snackBarService,
      this.sharedPolicyGroupsService,
      this.activatedRoute.snapshot.params.sharedPolicyGroupId,
      () => {
        this.router.navigate(['../..'], { relativeTo: this.activatedRoute });
      },
    );
  }
}
