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
  id?: string;
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

const HTTP_PROPERTIES_NODES: Record<string, NodeContainerElement> = {
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

const NODES_SORT = ['props', 'headers', 'attributes', 'body'];

const NODES: Record<string, NodeContainerElement> = {
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

  errorsTreeControl = new FlatTreeControl<FlatNode>(
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

  errorsDataSource = new MatTreeFlatDataSource(this.errorsTreeControl, this.treeFlattener as any);

  errorNodes: Node[];

  ngOnChanges(changes: SimpleChanges) {
    if (changes['inputDebugStep'] || changes['outputDebugStep']) {
      const { treeNodes, errorTreeNodes } = this.buildTreeNodes();
      this.dataSource.data = treeNodes;
      this.errorsDataSource.data = errorTreeNodes;
      if (errorTreeNodes.length > 0) {
        this.errorsTreeControl.expandAll();
      } else {
        this.treeControl.expandAll();
      }
    }
  }

  hasChild = (_: number, node: FlatNode) => node.expandable;

  private toErrorNode(errors: { key: string; value: any }[]): Node {
    return {
      type: 'error',
      input: errors,
    };
  }

  private toNode(nodes: Record<string, any>, key: string): Node {
    const { name, type } = nodes[key];
    const input = this.inputDebugStep.output[key];
    const output = this.outputDebugStep.output[key];
    return {
      id: key,
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

  buildTreeNodes(): { treeNodes: Node[]; errorTreeNodes: Node[] } {
    const keys = [...new Set(Object.keys(this.inputDebugStep.output).concat(Object.keys(this.outputDebugStep.output)))];

    const httpPropertiesTreeNodes: Node[] = keys
      .filter((key) => HTTP_PROPERTIES_NODES[key] != null)
      .sort(this.sort)
      .map((key) => this.toNode(HTTP_PROPERTIES_NODES, key));

    const treeNodes: Node[] = keys.filter((key) => NODES[key] != null).map((key) => this.toNode(NODES, key));

    if (httpPropertiesTreeNodes.length > 0) {
      treeNodes.push({
        id: 'props',
        name: 'HTTP properties',
        type: undefined,
        children: httpPropertiesTreeNodes,
      });
    }

    const errors = keys
      .filter((key) => key.startsWith('error.'))
      .sort(this.sort)
      .map((key) => ({ key, value: this.inputDebugStep.output[key] ?? this.outputDebugStep.output[key] }));
    const errorTreeNodes = [];
    if (errors.length > 0) {
      errorTreeNodes.push({
        name: 'Errors',
        type: 'error',
        children: [this.toErrorNode(errors)],
      });
    }

    return { treeNodes: treeNodes.sort((a, b) => NODES_SORT.indexOf(a.id) - NODES_SORT.indexOf(b.id)), errorTreeNodes };
  }

  private sort(a: string, b: string) {
    return a.toLowerCase().localeCompare(b.toLowerCase());
  }
}
