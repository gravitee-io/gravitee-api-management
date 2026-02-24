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
import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface ApiProductConfirmDeploymentDialogData {
  apiProductId: string;
}

export type ApiProductConfirmDeploymentDialogResult = true;

@Component({
  selector: 'api-product-confirm-deployment-dialog',
  templateUrl: './api-product-confirm-deployment-dialog.component.html',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
})
export class ApiProductConfirmDeploymentDialogComponent {
  private readonly dialogRef =
    inject<MatDialogRef<ApiProductConfirmDeploymentDialogComponent, ApiProductConfirmDeploymentDialogResult>>(MatDialogRef);

  onDeploy(): void {
    this.dialogRef.close(true);
  }
}
