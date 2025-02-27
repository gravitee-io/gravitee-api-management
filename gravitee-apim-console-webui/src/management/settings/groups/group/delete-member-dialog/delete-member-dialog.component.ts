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
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { Observable, startWith } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { MatListModule } from '@angular/material/list';

import { GroupMembership } from '../../../../../entities/group/groupMember';
import { RoleName } from '../membershipState';
import { Group, Member } from '../../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../../../services-ngx/users.service';
import { GroupService } from '../../../../../services-ngx/group.service';

@Component({
  selector: 'delete-member-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatAutocompleteModule,
    MatInputModule,
    MatListModule,
  ],
  templateUrl: './delete-member-dialog.component.html',
  styleUrl: './delete-member-dialog.component.scss',
})
export class DeleteMemberDialogComponent implements OnInit {
  member: Member;
  deleteMemberForm: FormGroup<{
    searchTerm: FormControl<string>;
  }>;
  ownershipTransferMessage: string;
  filteredMembers: Observable<Member[]>;
  selectedPrimaryOwner: Member;

  private members: Member[] = [];
  private updatedGroupMembership: GroupMembership;
  private group: Group;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private groupService: GroupService,
    private usersService: UsersService,
    private matDialogRef: MatDialogRef<DeleteMemberDialogComponent>,
    private snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.group = this.data['group'];
    this.member = this.data['member'];
    this.members = this.data['members'];
    this.deleteMemberForm = new FormGroup({
      searchTerm: new FormControl<string>(''),
    });
    this.filteredMembers = this.deleteMemberForm.controls['searchTerm'].valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      map((searchTerm) => this.filterMembers(searchTerm)),
    );
  }

  deleteMember(): void {
    this.groupService.deleteMember(this.group.id, this.member.id).subscribe({
      next: () => {
        this.snackBarService.success(`Successfully deleted ${this.member.displayName} from ${this.group.name}}`);
      },
      error: () => {
        this.snackBarService.success(`Error occurred while deleting ${this.member.displayName} from ${this.group.name}}`);
      },
    });

    if (this.updatedGroupMembership) {
      this.groupService.addOrUpdateMemberships(this.group.id, [this.updatedGroupMembership]).subscribe({
        next: () => {
          this.snackBarService.success(`Successfully transferred ownership to {{ ${this.selectedPrimaryOwner.displayName}.`);
        },
        error: () => {
          this.snackBarService.success(`Error occurred while transferring ownership from ${this.member.displayName}}`);
        },
      });
    }

    this.matDialogRef.close();
  }

  mapGroupMembership() {
    if (this.selectedPrimaryOwner) {
      this.usersService.search(this.selectedPrimaryOwner.displayName).subscribe({
        next: (response) => {
          const reference = response.find((user) => user.id === this.selectedPrimaryOwner.id).reference;
          this.ownershipTransferMessage = `${this.member.displayName} is the API primary owner. Primary ownership of the group will be transferred from ${this.member.displayName} to ${this.selectedPrimaryOwner.displayName}.`;
          this.updatedGroupMembership = {
            id: this.selectedPrimaryOwner.id,
            reference: reference,
            roles: [
              {
                name: RoleName.PRIMARY_OWNER,
                scope: 'API',
              },
              {
                name: this.selectedPrimaryOwner.roles['APPLICATION'],
                scope: 'APPLICATION',
              },
              {
                name: this.selectedPrimaryOwner.roles['INTEGRATION'],
                scope: 'INTEGRATION',
              },
            ],
          };
        },
      });
    }
  }

  private filterMembers(searchTerm: string): Member[] {
    if (!searchTerm.trim()) {
      return [];
    }

    const filterValue = searchTerm.toLowerCase();
    return this.members.filter((member) => member.displayName.toLowerCase().includes(filterValue) && member.id !== this.member.id);
  }

  selectPrimaryOwner($event: MatAutocompleteSelectedEvent) {
    this.selectedPrimaryOwner = $event.option.value;
    this.deleteMemberForm.controls['searchTerm'].setValue('');
    this.mapGroupMembership();
  }

  deselectPrimaryOwner() {
    this.selectedPrimaryOwner = null;
  }

  disableSubmit() {
    return this.member.roles['API'] === RoleName.PRIMARY_OWNER && !this.selectedPrimaryOwner;
  }
}
