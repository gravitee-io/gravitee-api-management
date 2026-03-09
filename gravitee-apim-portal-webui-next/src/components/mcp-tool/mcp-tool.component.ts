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
import { JsonPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';

import { McpTool } from '../../entities/api/mcp';
import { CopyCodeIconComponent } from '../copy-code/copy-code-icon/copy-code-icon/copy-code-icon.component';

@Component({
  selector: 'app-mcp-tool',
  standalone: true,
  imports: [MatExpansionModule, JsonPipe, ReactiveFormsModule, MatFormFieldModule, CopyCodeIconComponent],
  templateUrl: './mcp-tool.component.html',
  styleUrl: './mcp-tool.component.scss',
})
export class McpToolComponent {
  tool = input.required<McpTool>();
}
