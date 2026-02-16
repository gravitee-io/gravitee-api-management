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
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { AsyncPipe, NgForOf } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ClusterService } from '../../../../../services-ngx/cluster.service';
import { GroupV2Service } from '../../../../../services-ngx/group-v2.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';

export type ClusterManageGroupsDialogData = {
  clusterId: string;
};
export type ClusterManageGroupsDialogResult = undefined;

type GroupsVm = {
  id: string;
  name: string;
}[];

@Component({
  selector: 'cluster-manage-groups-dialog',
  templateUrl: './cluster-manage-groups-dialog.component.html',
  styleUrls: ['./cluster-manage-groups-dialog.component.scss'],
  imports: [
    MatFormFieldModule,
    ReactiveFormsModule,
    MatSelectModule,
    MatButtonModule,
    NgForOf,
    AsyncPipe,
    MatDialogActions,
    MatDialogContent,
    MatDialogTitle,
    GioPermissionModule,
    MatDialogClose,
  ],
})
export class ClusterManageGroupsDialogComponent implements OnInit {
  groupsControl?: FormControl<string[]> = undefined;

  dialogRef = inject(MatDialogRef<ClusterManageGroupsDialogComponent, ClusterManageGroupsDialogResult>);
  dialogData = inject<ClusterManageGroupsDialogData>(MAT_DIALOG_DATA);
  destroyRef = inject(DestroyRef);

  groupService = inject(GroupV2Service);
  clusterService = inject(ClusterService);
  snackBarService = inject(SnackBarService);

  groups$: Observable<GroupsVm> = this.groupService.list(1, 9999).pipe(
    map(groups =>
      groups.data.map(group => ({
        id: group.id,
        name: group.name,
      })),
    ),
  );

  readonlyGroupList$: Observable<string> = this.groupService.list(1, 9999).pipe(
    map(({ data }) => {
      if (!this.groupsControl?.value || this.groupsControl.value.length === 0) {
        return 'No groups associated';
      }
      return data
        .filter(g => this.groupsControl?.value?.includes(g.id))
        .map(g => g.name)
        .join(', ');
    }),
    startWith('Loading...'),
  );

  ngOnInit(): void {
    this.clusterService
      .get(this.dialogData.clusterId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(cluster => {
        this.groupsControl = new FormControl(cluster.groups);
      });
  }

  save() {
    this.clusterService
      .updateGroups(this.dialogData.clusterId, this.groupsControl?.value ? this.groupsControl.value : [])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackBarService.success('Cluster groups updated');
          this.dialogRef.close(undefined);
        },
        error: error => {
          this.snackBarService.error(error.error?.message ?? 'An error occurred while updating cluster groups');
        },
      });
  }
}
