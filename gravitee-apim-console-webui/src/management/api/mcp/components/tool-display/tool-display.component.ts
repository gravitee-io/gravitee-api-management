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
import { JsonPipe } from '@angular/common';
import { MatExpansionModule } from '@angular/material/expansion';

import { MCPToolDefinition } from '../../../../../entities/entrypoint/mcp';

@Component({
  selector: 'tool-display',
  imports: [JsonPipe, MatExpansionModule],
  templateUrl: './tool-display.component.html',
  styleUrl: './tool-display.component.scss',
})
export class ToolDisplayComponent {
  tool = input.required<MCPToolDefinition>();
}
