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

import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FlatTreeControl } from '@angular/cdk/tree';
import { MatTreeFlatDataSource, MatTreeFlattener } from '@angular/material/tree';

import { RequestDebugStep, ResponseDebugStep } from '../../models/DebugStep';

interface Node {
  name?: string;
  children?: Node[];
  type?: string;
  input?: any;
  output?: any;
}

interface FlatNode {
  expandable: boolean;
  name: string;
  level: number;
}

interface NodeContainerElement {
  name: string;
  type?: string;
}

const httpProperties: Record<string, NodeContainerElement> = {
  parameters: {
    name: 'Query params',
    type: 'text',
  },
  pathParameters: {
    name: 'Path params',
    type: 'table',
  },
  method: {
    name: 'Method',
    type: 'text',
  },
  path: {
    name: 'Path',
    type: 'text',
  },
  contextPath: {
    name: 'Context path',
    type: 'text',
  },
};

const nodes: Record<string, NodeContainerElement> = {
  headers: {
    name: 'HTTP headers',
    type: 'table',
  },
  body: {
    name: 'HTTP body',
    type: 'body',
  },
  attributes: {
    name: 'Attributes',
    type: 'table',
  },
};

export function getDiffState(input: string, output: string): 'added' | 'deleted' | 'updated' {
  if (!input && !!output) {
    return 'added';
  } else if (!!input && !output) {
    return 'deleted';
  } else if (!!input && !!output && input !== output) {
    return 'updated';
  }
  return undefined;
}

@Component({
  selector: 'policy-studio-debug-inspector',
  template: require('./policy-studio-debug-inspector.component.html'),
  styles: [require('./policy-studio-debug-inspector.component.scss')],
})
export class PolicyStudioDebugInspectorComponent implements OnChanges {
  @Input()
  inputDebugStep: RequestDebugStep | ResponseDebugStep;

  @Input()
  outputDebugStep: RequestDebugStep | ResponseDebugStep;

  private transformer = (node: Node, level: number) => {
    return {
      ...node,
      expandable: !!node.children && node.children.length > 0,
      level: level,
    };
  };

  treeControl = new FlatTreeControl<FlatNode>(
    (node) => node.level,
    (node) => node.expandable,
  );

  treeFlattener = new MatTreeFlattener(
    this.transformer,
    (node) => node.level,
    (node) => node.expandable,
    (node) => node.children,
  );

  // FIXME: remove as any
  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener as any);

  ngOnChanges(changes: SimpleChanges) {
    if (changes['inputDebugStep'] || changes['outputDebugStep']) {
      this.dataSource.data = this.buildTreeNodes();
      this.treeControl.expandAll();
    }
  }

  hasChild = (_: number, node: FlatNode) => node.expandable;

  private toErrorNode(key: string): Node {
    const input = this.inputDebugStep.output[key];
    const output = this.outputDebugStep.output[key];
    const name = key.replace('error.', '');
    return {
      name,
      type: 'error',
      input,
      output,
    };
  }

  private toNode(nodes: Record<string, any>, key: string): Node {
    const { name, type } = nodes[key];
    const input = this.inputDebugStep.output[key];
    const output = this.outputDebugStep.output[key];
    return {
      name,
      type: undefined,
      children: [
        {
          name,
          type,
          input,
          output,
        },
      ],
    };
  }

  buildTreeNodes(): Node[] {
    const keys = [...new Set(Object.keys(this.inputDebugStep.output).concat(Object.keys(this.outputDebugStep.output)))];

    const httpPropertiesNodes: Node[] = keys.filter((key) => httpProperties[key] != null).map((key) => this.toNode(httpProperties, key));

    let data: Node[] = keys.filter((key) => nodes[key] != null).map((key) => this.toNode(nodes, key));

    if (httpPropertiesNodes.length > 0) {
      data.push({
        name: 'HTTP properties',
        type: undefined,
        children: httpPropertiesNodes,
      });
    }

    data = data.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));

    const errors = keys.filter((key) => key.startsWith('error.')).map((key) => this.toErrorNode(key));
    if (errors.length > 0) {
      data.unshift({
        name: 'Errors',
        type: undefined,
        children: errors,
      });
    }
    return data;
  }
}
