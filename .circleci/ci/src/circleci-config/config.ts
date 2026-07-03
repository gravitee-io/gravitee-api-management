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
import { stringify } from 'yaml';
import { Job } from './job';
import { OrbImport } from './orb';
import { AnyParameter, CustomEnumParameter, CustomParameter, CustomParametersList } from './parameters';
import { ParameterizedJob, ReusableCommand } from './reusable';
import { Generable, ParameterType, Schema } from './types';
import { Workflow } from './workflow';

const HEADER = [
  '# This file is generated from the TypeScript sources in .circleci/ci.',
  '# Do not edit it by hand — run `npm run generate` instead.',
  '',
  '',
].join('\n');

/** Merge the `{ name: body }` fragments produced by a list of components. */
function mergeGenerated(components: Generable[]): Schema {
  return components.reduce<Schema>((acc, component) => Object.assign(acc, component.generate()), {});
}

/**
 * A CircleCI configuration. Collects parameters, orbs, reusable commands, jobs
 * and workflows (deduplicated by name/alias) and serializes them to YAML in the
 * canonical section order.
 */
export class Config implements Generable {
  private readonly parameters: AnyParameter[] = [];
  private readonly orbs = new Map<string, OrbImport>();
  private readonly commands = new Map<string, ReusableCommand>();
  private readonly jobs = new Map<string, Job | ParameterizedJob>();
  private readonly workflows: Workflow[] = [];

  defineParameter(name: string, type: ParameterType, defaultValue?: unknown, description = '', enumValues?: string[]): this {
    this.parameters.push(
      enumValues
        ? new CustomEnumParameter(name, enumValues, defaultValue, description)
        : new CustomParameter(name, type, defaultValue, description),
    );
    return this;
  }

  importOrb(orb: OrbImport): this {
    this.orbs.set(orb.alias, orb);
    return this;
  }

  addReusableCommand(command: ReusableCommand): this {
    this.commands.set(command.name, command);
    return this;
  }

  addJob(job: Job | ParameterizedJob): this {
    this.jobs.set(job.name, job);
    return this;
  }

  addWorkflow(workflow: Workflow): this {
    this.workflows.push(workflow);
    return this;
  }

  generate(): Schema {
    const doc: Schema = { version: 2.1, setup: false };
    if (this.parameters.length > 0) {
      doc.parameters = new CustomParametersList(this.parameters).generate();
    }
    if (this.commands.size > 0) {
      doc.commands = mergeGenerated([...this.commands.values()]);
    }
    if (this.jobs.size > 0) {
      doc.jobs = mergeGenerated([...this.jobs.values()]);
    }
    if (this.workflows.length > 0) {
      doc.workflows = mergeGenerated(this.workflows);
    }
    if (this.orbs.size > 0) {
      doc.orbs = mergeGenerated([...this.orbs.values()]);
    }
    return doc;
  }

  stringify(): string {
    return HEADER + stringify(this.generate(), { lineWidth: 0 });
  }
}
