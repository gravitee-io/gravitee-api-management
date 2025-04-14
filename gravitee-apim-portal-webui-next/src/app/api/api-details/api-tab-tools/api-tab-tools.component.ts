<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 47db69a114 (style: apply prettier)
/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
<<<<<<< HEAD
import { JsonPipe } from '@angular/common';
=======
>>>>>>> 47db69a114 (style: apply prettier)
import { Component, computed, input } from '@angular/core';

import { McpToolComponent } from '../../../../components/mcp-tool/mcp-tool.component';
import { Api } from '../../../../entities/api/api';
<<<<<<< HEAD

@Component({
  selector: 'app-api-tab-tools',
  imports: [McpToolComponent, JsonPipe],
  templateUrl: './api-tab-tools.component.html',
  styleUrl: './api-tab-tools.component.scss',
=======
import {Component, computed, input, Input} from '@angular/core';
import {Api} from "../../../../entities/api/api";
import {McpToolComponent} from "../../../../components/mcp-tool/mcp-tool.component";
=======
>>>>>>> 47db69a114 (style: apply prettier)

@Component({
  selector: 'app-api-tab-tools',
  standalone: true,
  imports: [McpToolComponent],
  templateUrl: './api-tab-tools.component.html',
<<<<<<< HEAD
  styleUrl: './api-tab-tools.component.scss'
>>>>>>> 5559b8d249 (--wip-- [skip ci])
=======
  styleUrl: './api-tab-tools.component.scss',
>>>>>>> 47db69a114 (style: apply prettier)
})
export class ApiTabToolsComponent {
  api = input.required<Api>();

<<<<<<< HEAD
  mcpTools = computed(() => {
    return this.api().mcp?.tools ?? [];
  });
=======
  mcpTools = computed(() => this.api().mcp?.tools ?? []);
>>>>>>> 5559b8d249 (--wip-- [skip ci])
}
