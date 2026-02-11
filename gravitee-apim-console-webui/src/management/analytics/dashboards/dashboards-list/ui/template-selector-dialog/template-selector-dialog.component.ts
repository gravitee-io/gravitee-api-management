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
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule, KeyValuePipe, NgOptimizedImage } from '@angular/common';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { ALL_DASHBOARD_TEMPLATES, DashboardTemplate } from '../../../../data-access/templates';

export interface TemplateSelectorDialogResult {
  template: DashboardTemplate;
}

@Component({
  selector: 'template-selector-dialog',
  templateUrl: './template-selector-dialog.component.html',
  styleUrls: ['./template-selector-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, KeyValuePipe, MatDialogModule, MatButtonModule, MatIconModule, GioIconsModule, NgOptimizedImage],
})
export class TemplateSelectorDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<TemplateSelectorDialogComponent>);

  public readonly templates = ALL_DASHBOARD_TEMPLATES;
  public readonly selectedTemplate = signal<DashboardTemplate>(this.templates[0]);

  selectTemplate(template: DashboardTemplate): void {
    this.selectedTemplate.set(template);
  }

  isSelected(template: DashboardTemplate): boolean {
    return this.selectedTemplate()?.id === template.id;
  }

  onCreate(): void {
    this.dialogRef.close({ template: this.selectedTemplate() } as TemplateSelectorDialogResult);
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
