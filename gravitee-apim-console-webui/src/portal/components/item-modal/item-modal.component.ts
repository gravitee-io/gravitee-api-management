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
import { Component, inject, Input } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogActions, MatDialogContent, MatDialogRef } from '@angular/material/dialog';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatButton } from '@angular/material/button';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatInput } from '@angular/material/input';

export type ItemType = 'folder' | 'page' | 'link';
export type ItemMode = 'add' | 'edit' | 'delete';

@Component({
  selector: 'app-item-modal',
  standalone: true,
  templateUrl: './item-modal.component.html',
  styleUrls: ['./item-modal.component.scss'],
  imports: [MatDialogContent, MatFormField, MatButton, MatLabel, MatSlideToggle, ReactiveFormsModule, MatDialogActions, MatInput],
})
export class ItemModalComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<ItemModalComponent>);

  @Input() type!: ItemType;
  @Input() mode: ItemMode = 'add';
  @Input() initialData: any;
  @Input() extraInfo?: string;

  form = this.fb.group({
    title: ['', Validators.required],
    url: [''],
    authRequired: [false],
  });

  ngOnInit() {
    if (this.initialData) {
      this.form.patchValue(this.initialData);
    }

    if (this.mode === 'delete') {
      this.form.disable();
    }
  }

  get dialogTitle() {
    const prefix = this.mode === 'add' ? 'Add' : this.mode === 'edit' ? 'Edit' : 'Delete this';

    return this.mode === 'delete' ? `${prefix} ${this.type}?` : `${prefix} ${this.type}`;
  }

  get titleLabel() {
    switch (this.type) {
      case 'folder':
        return 'Folder title*';
      case 'page':
        return 'Page title*';
      default:
        return 'Link title*';
    }
  }

  get showUrlField() {
    return this.type === 'link' && this.mode !== 'delete';
  }

  get primaryButtonLabel() {
    switch (this.mode) {
      case 'add':
        return 'Add';
      case 'edit':
        return 'Save';
      case 'delete':
        return 'Delete';
    }
  }

  get primaryButtonColor() {
    return this.mode === 'delete' ? 'warn' : 'primary';
  }

  get message(): string | null {
    if (this.mode !== 'delete') return null;
    switch (this.type) {
      case 'folder':
        return this.extraInfo ? `This folder ${this.extraInfo}.` : 'This folder will be permanently deleted.';
      case 'page':
        return 'This page will no longer appear on your site.';
      case 'link':
        return 'This link will no longer appear on your developer portal.';
      default:
        return '';
    }
  }

  onCancel() {
    this.dialogRef.close();
  }

  onSubmit() {
    if (this.mode === 'delete') {
      this.dialogRef.close(true);
    } else if (this.form.valid) {
      this.dialogRef.close(this.form.value);
    }
  }
}
