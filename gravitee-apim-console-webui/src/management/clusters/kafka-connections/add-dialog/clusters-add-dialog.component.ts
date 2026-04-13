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
import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { GioFormFocusInvalidModule } from '@gravitee/ui-particles-angular';
import { map, startWith } from 'rxjs/operators';
import { Observable } from 'rxjs';

export type ClustersAddDialogData = undefined;
export type ClustersAddDialogResult = undefined | { name: string; description?: string; bootstrapServers: string };

@Component({
  selector: 'clusters-add-dialog',
  templateUrl: './clusters-add-dialog.component.html',
  styleUrls: ['./clusters-add-dialog.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInput, GioFormFocusInvalidModule],
  standalone: true,
})
export class ClustersAddDialogComponent {
  protected formGroup: FormGroup<{
    name: FormControl<string>;
    description: FormControl<string>;
    bootstrapServers: FormControl<string>;
  }>;
  protected isValid$: Observable<boolean>;

  constructor(public dialogRef: MatDialogRef<ClustersAddDialogComponent, ClustersAddDialogResult>) {
    this.formGroup = new FormGroup({
      name: new FormControl('', Validators.required),
      description: new FormControl(''),
      bootstrapServers: new FormControl('', Validators.required),
    });

    this.isValid$ = this.formGroup.statusChanges.pipe(
      startWith(this.formGroup.status),
      map(status => status === 'VALID'),
    );
  }

  protected onSave(): void {
    if (this.formGroup.invalid) {
      return;
    }
    this.dialogRef.close({
      name: this.formGroup.get('name').value,
      description: this.formGroup.get('description').value,
      bootstrapServers: this.formGroup.get('bootstrapServers').value,
    });
  }
}
