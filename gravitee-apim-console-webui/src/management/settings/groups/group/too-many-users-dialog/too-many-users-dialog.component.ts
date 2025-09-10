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
import { MatCardModule } from '@angular/material/card';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { GioBannerModule } from '@gravitee/ui-particles-angular';

import { TooManyUsersDialogData } from '../group.component';

@Component({
  selector: 'too-many-users-dialog',
  imports: [MatCardModule, MatDialogModule, MatButtonModule, GioBannerModule],
  templateUrl: './too-many-users-dialog.component.html',
  styleUrl: './too-many-users-dialog.component.scss',
})
export class TooManyUsersDialogComponent implements OnInit {
  email: string;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: TooManyUsersDialogData,
    private matDialogRef: MatDialogRef<TooManyUsersDialogComponent>,
  ) {}

  ngOnInit(): void {
    this.email = this.data.email;
  }

  submit() {
    this.matDialogRef.close(true);
  }
}
