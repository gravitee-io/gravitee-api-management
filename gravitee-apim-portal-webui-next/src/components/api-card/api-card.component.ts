/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, Input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';

import { PictureComponent } from '../picture/picture.component';

@Component({
  selector: 'app-api-card',
  imports: [MatCardModule, MatButtonModule, PictureComponent, RouterModule, MatChipsModule, MatTooltip],
  templateUrl: './api-card.component.html',
  styleUrl: './api-card.component.scss',
})
export class ApiCardComponent {
  @Input({ required: true })
  title!: string;
  @Input({ required: true })
  version!: string;
  @Input({ required: true })
  id!: string;
  @Input()
  picture: string | undefined;
  @Input()
  isEnabledMcpServer: boolean = false;
  @Input()
  content?: string;
}
