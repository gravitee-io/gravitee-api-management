<<<<<<< HEAD
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
import { JsonPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';

import { McpTool } from '../../entities/api/mcp';
import { CopyCodeIconComponent } from '../copy-code/copy-code-icon/copy-code-icon/copy-code-icon.component';
=======
import {Component, computed, input, OnInit, Signal} from '@angular/core';
import {MCPTool} from "../../entities/api/mcp";
import {MatExpansionModule} from "@angular/material/expansion";
import {JsonPipe} from "@angular/common";
import {MatFormFieldModule} from "@angular/material/form-field";
import {FormControl, ReactiveFormsModule, UntypedFormGroup} from "@angular/forms";
import {MatInput} from "@angular/material/input";
import {toSignal} from "@angular/core/rxjs-interop";
import { map} from "rxjs";
import {CopyCodeIconComponent} from "../copy-code/copy-code-icon/copy-code-icon/copy-code-icon.component";

interface RequestVM {
  jsonrpc: string;
  id: number;
  method: string;
  params: {
    name: string;
    arguments: Arguments;
  }
}

interface Arguments {
  [property: string]: string;
}

interface PropertyVM {
  name: string;
  description?: string;
  value?: string;
  required?: boolean;
}

<<<<<<< HEAD
interface DisplayVMs {
  [property: string]: string;
}
>>>>>>> 5559b8d249 (--wip-- [skip ci])

@Component({
  selector: 'app-mcp-tool',
  standalone: true,
<<<<<<< HEAD
  imports: [MatExpansionModule, JsonPipe, ReactiveFormsModule, MatFormFieldModule, CopyCodeIconComponent],
  templateUrl: './mcp-tool.component.html',
  styleUrl: './mcp-tool.component.scss',
})
export class McpToolComponent {
  tool = input.required<McpTool>();
=======
  imports: [MatExpansionModule, JsonPipe, ReactiveFormsModule, MatFormFieldModule, MatInput],
=======
@Component({
  selector: 'app-mcp-tool',
  standalone: true,
  imports: [MatExpansionModule, JsonPipe, ReactiveFormsModule, MatFormFieldModule, MatInput, CopyCodeIconComponent],
>>>>>>> ab1918adc5 (feat(portal-next): show tools when mcp enabled)
  templateUrl: './mcp-tool.component.html',
  styleUrl: './mcp-tool.component.scss'
})
export class McpToolComponent implements OnInit {
  tool = input.required<MCPTool>();

  toolToPropertyVMs: Signal<PropertyVM[]> = computed(() => {
    const properties = this.tool().inputSchema.properties ?? {};
    const requiredProperties = this.tool().inputSchema?.required ?? [];
    if (Object.entries(properties).length) {
      return Object.entries(properties)
        .map(([key, property]) =>
          ({name: key, description: property.description, value: '', required: this.tool().inputSchema.required && requiredProperties.includes(key)}))
    }
    return [];
  })

  form: UntypedFormGroup = new UntypedFormGroup({});

  args: Signal<Arguments | undefined> = toSignal(this.form.valueChanges.pipe(
    map((values) => {
      const args: Arguments = {};
      Object.entries(values).forEach(([key, value]) => {
        if (typeof value === "string") {
          args[key] = value
        }
      })
      return args;
    })));

  display: Signal<RequestVM> = computed(() => {
    const args: Arguments = this.args() ?? {};
        return {
          jsonrpc: "2.0",
          id: 2,
          method: "tools/call",
          params: {
            name: this.tool().name,
            arguments: args,
          }
      };
  })

  ngOnInit() {
    const propertyVms = this.toolToPropertyVMs();
    propertyVms.forEach((property) => {
      this.form.addControl(property.name, new FormControl(''));
    })
  }
>>>>>>> 5559b8d249 (--wip-- [skip ci])
}
