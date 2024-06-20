import { ChangeDetectorRef, Component, Inject, OnInit } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle
} from '@angular/material/dialog';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { NgIf } from '@angular/common';
import { GioFormSlideToggleModule, GioFormTagsInputModule } from '@gravitee/ui-particles-angular';
import { MatSlideToggle } from '@angular/material/slide-toggle';

import { CustomUserField } from '../../../../../entities/customUserFields';

export interface CustomUserFieldsDialogData {
  action: 'Update' | 'Create';
  key?: string;
  label?: string;
  required?: boolean;
  values?: string[];
}

export type CustomUserFieldsDialogResult = CustomUserField;

@Component({
  selector: 'app-custom-user-fields-dialog',
  standalone: true,
  imports: [
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatButton,
    ReactiveFormsModule,
    MatError,
    MatFormField,
    MatInput,
    MatLabel,
    NgIf,
    GioFormSlideToggleModule,
    MatSlideToggle,
    GioFormTagsInputModule
  ],
  templateUrl: './custom-user-fields-dialog.component.html',
  styleUrl: './custom-user-fields-dialog.component.scss'
})
export class CustomUserFieldsDialogComponent implements OnInit {
  public customUserFieldsDialogData: CustomUserFieldsDialogData;
  public form;
  public isUpdate = false;

  constructor(
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly dialogRef: MatDialogRef<CustomUserFieldsDialogData, CustomUserFieldsDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: CustomUserFieldsDialogData,
    private readonly formBuilder: UntypedFormBuilder
  ) {
    this.customUserFieldsDialogData = dialogData;
  }

  ngOnInit() {
    this.isUpdate = this.customUserFieldsDialogData.action === 'Update';
    this.buildForm();
  }

  private buildForm() {
    this.form = this.formBuilder.group({
      key: [{
        value: '',
        disabled: this.isUpdate
      }, [Validators.required, Validators.maxLength(50), Validators.minLength(1)]],
      label: ['', [Validators.required, Validators.maxLength(50), Validators.minLength(1)]],
      required: [false],
      values: [[]]
    });
    if (this.isUpdate) {
      this.seedData();
    }
  }

  private seedData() {
    const {
      key,
      label,
      required,
      values
    } = this.customUserFieldsDialogData;

    this.form.patchValue({
      key,
      label,
      required,
      values
    });
  }

  public onClose() {
    this.dialogRef.close();
  }

  public save() {
    this.dialogRef.close({ key: this.customUserFieldsDialogData.key, ...this.form.value });
  }
}
