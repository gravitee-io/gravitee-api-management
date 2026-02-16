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
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { Observable, of, startWith } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { MatListModule } from '@angular/material/list';
import { MatChip, MatChipRemove, MatChipSet } from '@angular/material/chips';
import { GioBannerModule } from '@gravitee/ui-particles-angular';

import { GroupMembership } from '../../../../../entities/group/groupMember';
import { Member, RoleName } from '../membershipState';
import { UsersService } from '../../../../../services-ngx/users.service';
import { DeleteMemberDialogData } from '../group.component';

@Component({
  selector: 'delete-member-dialog',
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
    MatChip,
    MatChipRemove,
    MatChipSet,
    GioBannerModule,
  ],
  templateUrl: './delete-member-dialog.component.html',
  styleUrl: './delete-member-dialog.component.scss',
})
export class DeleteMemberDialogComponent implements OnInit {
  member: Member = null;
  filteredMembers$: Observable<Member[]> = of([]);
  newPrimaryOwner: Member = null;
  deleteMemberForm: FormGroup<{
    searchTerm: FormControl<string>;
  }>;
  ownershipTransferMessage: string = null;
  disableSubmit = false;

  private members: Member[] = [];
  private primaryOwnerMembership: GroupMembership = null;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: DeleteMemberDialogData,
    private usersService: UsersService,
    private matDialogRef: MatDialogRef<DeleteMemberDialogComponent>,
  ) {}

  ngOnInit(): void {
    this.initializeDataFromInput();
    this.initializeForm();
    this.initializeFilterFn();
  }

  private initializeDataFromInput() {
    this.member = this.data.member;
    this.members = this.data.members;
  }

  private initializeForm() {
    this.deleteMemberForm = new FormGroup({
      searchTerm: new FormControl<string>({ value: '', disabled: true }),
    });
    const isPrimaryOwner = this.member.roles['API'] === RoleName.PRIMARY_OWNER;

    if (isPrimaryOwner) {
      this.deleteMemberForm.controls.searchTerm.enable();
      this.deleteMemberForm.controls.searchTerm.addValidators(Validators.required);
      this.disableSubmit = true;
    }
  }

  private initializeFilterFn() {
    this.filteredMembers$ = this.deleteMemberForm.controls.searchTerm.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      map(searchTerm => this.filterMembers(searchTerm)),
    );
  }

  private filterMembers(searchTerm: string): Member[] {
    if (!searchTerm.trim()) {
      return [];
    }

    const filterValue = searchTerm.toLowerCase();
    return this.members.filter(member => member.displayName.toLowerCase().includes(filterValue) && member.id !== this.member.id);
  }

  submit(): void {
    this.matDialogRef.close({ primaryOwnerMembership: this.primaryOwnerMembership, shouldDelete: true });
  }

  selectPrimaryOwner($event: MatAutocompleteSelectedEvent) {
    this.newPrimaryOwner = $event.option.value;
    this.deleteMemberForm.controls.searchTerm.setValue('');
    this.deleteMemberForm.controls.searchTerm.disable();
    this.deleteMemberForm.controls.searchTerm.removeValidators(Validators.required);
    this.ownershipTransferMessage = `${this.member.displayName} is the API primary owner. Primary ownership of the group will be transferred from ${this.member.displayName} to ${this.newPrimaryOwner.displayName}.`;
    this.mapGroupMembership();
    this.disableSubmit = false;
  }

  deselectPrimaryOwner() {
    this.newPrimaryOwner = null;
    this.ownershipTransferMessage = null;
    this.primaryOwnerMembership = null;
    this.deleteMemberForm.controls.searchTerm.setValue('');
    this.deleteMemberForm.controls.searchTerm.enable();
    this.deleteMemberForm.controls.searchTerm.addValidators(Validators.required);
    this.disableSubmit = true;
  }

  private mapGroupMembership() {
    this.usersService.search(this.newPrimaryOwner.displayName).subscribe({
      next: response => {
        const reference = response.find(user => user.id === this.newPrimaryOwner.id).reference;
        this.primaryOwnerMembership = {
          id: this.newPrimaryOwner.id,
          reference: reference,
          roles: [
            {
              name: RoleName.PRIMARY_OWNER,
              scope: 'API',
            },
            {
              name: this.newPrimaryOwner.roles['APPLICATION'],
              scope: 'APPLICATION',
            },
            {
              name: this.newPrimaryOwner.roles['INTEGRATION'],
              scope: 'INTEGRATION',
            },
          ],
        };
      },
    });
  }
}
