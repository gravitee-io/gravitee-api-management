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
import { Component, Inject, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Subject, takeUntil } from 'rxjs';
import { Constants } from 'src/entities/Constants';
import { Group, Member } from 'src/entities/management-api-v2';
import { TransferOwnershipDialogData } from 'src/permissions/model/transfer-ownership-dialog-data';
import { TransferOwnershipMode } from 'src/permissions/model/transfer-ownership-mode';

@Component({
  selector: 'cluster-transfer-ownership',
  templateUrl: './cluster-transfer-ownership.component.html',
  styleUrls: ['./cluster-transfer-ownership.component.scss'],
  standalone: false,
})
export class ClusterTransferOwnershipComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  form: FormGroup;
  mode: TransferOwnershipMode = 'USER';
  groups: Group[];
  poGroups: Group[];
  entityMembers: Member[];

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    @Inject(MAT_DIALOG_DATA) dialogData: TransferOwnershipDialogData,
  ) {
    this.groups = dialogData.groups;
    this.entityMembers = dialogData.members.filter((member) => !member.roles?.map((r) => r.name)?.includes('PRIMARY_OWNER'));
  }

  ngOnInit(): void {
    this.poGroups = this.groups.filter((group) => group.apiPrimaryOwner != null);
    this.initForm();
  }

  private initForm() {
    this.form = new FormGroup(
      {
        transferMode: new FormControl('ENTITY_MEMBER'),
        user: new FormControl(),
        groupId: new FormControl(),
        roleId: new FormControl(),
      },
      [
      ],
    );
  }

  onSubmit() {}
}
