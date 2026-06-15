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
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

import { PortalNavigationItem } from '../../../entities/management-api-v2';

export interface PublishNavigationItemDialogData {
  navItem: PortalNavigationItem;
}

export interface PublishNavigationItemDialogResult {
  confirmed: true;
  propagatePublishToChildren: boolean;
}

interface PublishNavigationItemDialogFormControls {
  propagatePublishToChildren: FormControl<boolean>;
}

type PublishNavigationItemDialogForm = FormGroup<PublishNavigationItemDialogFormControls>;

@Component({
  selector: 'publish-navigation-item-dialog',
  imports: [MatDialogModule, ReactiveFormsModule, MatButtonModule, MatCheckboxModule],
  templateUrl: './publish-navigation-item-dialog.component.html',
  styleUrls: ['./publish-navigation-item-dialog.component.scss'],
})
export class PublishNavigationItemDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<PublishNavigationItemDialogComponent, PublishNavigationItemDialogResult>);
  private readonly data: PublishNavigationItemDialogData = inject(MAT_DIALOG_DATA);

  private readonly navItem = signal(this.data.navItem);
  readonly form: PublishNavigationItemDialogForm = new FormGroup<PublishNavigationItemDialogFormControls>({
    propagatePublishToChildren: new FormControl(false, { nonNullable: true }),
  });

  readonly isContainer = computed(() => this.navItem().type === 'FOLDER' || this.navItem().type === 'API');
  readonly typeLabel = computed(() => (this.navItem().type === 'API' ? 'API' : this.navItem().type.toLowerCase()));
  readonly isPublishing = computed(() => !this.navItem().published);
  readonly action = computed(() => (this.isPublishing() ? 'Publish' : 'Unpublish'));
  readonly showPropagationCheckbox = computed(() => this.isPublishing() && this.isContainer());
  readonly title = computed(() => `${this.action()} "${this.navItem().title}" ${this.typeLabel()}?`);
  readonly pastAction = computed(() => `${this.action().toLowerCase()}ed`);
  readonly contentScope = computed(() => (this.isContainer() ? ' and its content ' : ' '));
  readonly propagatedItemsLabel = computed(() =>
    this.navItem().type === 'FOLDER' ? 'nested documentation and APIs' : 'nested documentation',
  );
  readonly propagationCheckboxLabel = computed(() => `Also publish all ${this.propagatedItemsLabel()}`);
  readonly warning = computed(() => {
    if (!this.isContainer()) {
      return '';
    }
    return this.isPublishing()
      ? ` Publishing this ${this.typeLabel()} will also publish all ${this.propagatedItemsLabel()}. Do you want to proceed?`
      : ` Unpublishing this ${this.typeLabel()} will also unpublish all ${this.propagatedItemsLabel()}. This action cannot be undone automatically. Do you want to proceed?`;
  });
  readonly messageWithPropagation = computed(
    () => `This ${this.typeLabel()} will be ${this.pastAction()}. This change will be visible in the Developer Portal.`,
  );
  readonly messageWithoutPropagation = computed(
    () =>
      `This ${this.typeLabel()}${this.contentScope()}will be ${this.pastAction()}. This change will be visible in the Developer Portal.${this.warning()}`,
  );
  readonly message = computed(() => (this.showPropagationCheckbox() ? this.messageWithPropagation() : this.messageWithoutPropagation()));

  onConfirm(): void {
    this.dialogRef.close({
      confirmed: true,
      propagatePublishToChildren: this.showPropagationCheckbox() ? this.form.controls.propagatePublishToChildren.value : false,
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
