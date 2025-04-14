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
import { Component, input } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { GioMonacoEditorModule } from '@gravitee/ui-particles-angular';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { JsonPipe } from '@angular/common';

import { MCPTool } from '../../../../entities/management-api-v2';

@Component({
  selector: 'mcp-tool-read',
  standalone: true,
  imports: [MatExpansionModule, GioMonacoEditorModule, ReactiveFormsModule, FormsModule, JsonPipe],
  templateUrl: './mcp-tool-display.component.html',
  styleUrl: './mcp-tool-display.component.scss',
})
export class McpToolDisplayComponent {
  tool = input.required<MCPTool>();
}
