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
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { GioFormSelectionInlineModule } from '@gravitee/ui-particles-angular';
import { NgIf } from '@angular/common';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';

import {
  PORTAL_MENU_LINK_TYPES,
  PortalMenuLinkType,
  PortalMenuLinkVisibility,
  toReadableMenuLinkType,
} from '../../../../entities/management-api-v2';

export interface MenuLinkAddDialogData {}

export interface MenuLinkAddDialogResult {
  name: string;
  type: PortalMenuLinkType;
  target: string;
  visibility: PortalMenuLinkVisibility;
}

@Component({
  selector: 'menu-link-add-dialog',
  templateUrl: './menu-link-add-dialog.component.html',
  styleUrls: ['./menu-link-add-dialog.component.scss'],
  standalone: true,
  imports: [
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
    GioFormSelectionInlineModule,
    NgIf,
    MatButtonToggle,
    MatButtonToggleGroup,
  ],
})
export class MenuLinkAddDialogComponent {
  public linksType = [];

  form = new FormGroup({
    name: new FormControl('', [Validators.required]),
    type: new FormControl<PortalMenuLinkType>('EXTERNAL', [Validators.required]),
    target: new FormControl('', [Validators.required]),
    visibility: new FormControl<PortalMenuLinkVisibility>('PUBLIC', [Validators.required]),
  });

  constructor(private readonly dialogRef: MatDialogRef<MenuLinkAddDialogData, MenuLinkAddDialogResult>) {
    this.linksType = PORTAL_MENU_LINK_TYPES.map((type) => {
      return {
        name: toReadableMenuLinkType(type),
        type,
        disabled: false,
      };
    });
    this.linksType.push({
      name: 'Link to a Guide Page',
      type: undefined,
      disabled: true,
    });
  }

  onSubmit() {
    const { name, type, target, visibility } = this.form.getRawValue();
    this.dialogRef.close({
      name,
      type,
      target,
      visibility,
    });
  }
}
