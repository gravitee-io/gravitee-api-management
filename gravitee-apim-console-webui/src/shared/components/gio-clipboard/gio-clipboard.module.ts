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
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatRippleModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ClipboardModule } from '@angular/cdk/clipboard';

import { GioClipboardCopyWrapperComponent } from './gio-clipboard-copy-wrapper.component';
import { GioClipboardCopyIconComponent } from './gio-clipboard-copy-icon.component';

@NgModule({
  imports: [CommonModule, MatIconModule, MatTooltipModule, MatRippleModule, ClipboardModule],
  declarations: [GioClipboardCopyWrapperComponent, GioClipboardCopyIconComponent],
  exports: [GioClipboardCopyWrapperComponent, GioClipboardCopyIconComponent],
})
export class GioClipboardModule {}
