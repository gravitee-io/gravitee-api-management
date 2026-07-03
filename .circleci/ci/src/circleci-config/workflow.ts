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
import { Job } from './job';
import { OrbRef } from './orb';
import { ParameterizedJob } from './reusable';
import { Command, Generable, Schema } from './types';

/**
 * Options of a job entry inside a workflow. Keys are emitted in insertion
 * order; `preSteps` becomes `pre-steps` and `matrix` is wrapped under
 * `parameters`. Any other key (name, context, requires, orb params, …) is
 * passed through unchanged.
 */
export interface WorkflowJobParameters {
  name?: string;
  context?: string[];
  requires?: string[];
  preSteps?: Command[];
  matrix?: Record<string, unknown>;
  [key: string]: unknown;
}

/** A job reference inside a workflow. Emits the bare name or `{ name: options }`. */
export class WorkflowJob implements Generable {
  constructor(
    private readonly job: Job | ParameterizedJob | OrbRef,
    private readonly parameters?: WorkflowJobParameters,
  ) {}

  generate(): string | Schema {
    if (!this.parameters || Object.keys(this.parameters).length === 0) {
      return this.job.name;
    }

    const body: Schema = {};
    for (const [key, value] of Object.entries(this.parameters)) {
      if (value === undefined) {
        continue;
      }
      if (key === 'preSteps') {
        body['pre-steps'] = (value as Command[]).map((step) => step.generate());
      } else if (key === 'matrix') {
        body.matrix = { parameters: value };
      } else {
        body[key] = value;
      }
    }
    return { [this.job.name]: body };
  }
}

/** A workflow (entry of the `workflows:` section). Emits `{ name: { jobs } }`. */
export class Workflow implements Generable {
  constructor(
    public readonly name: string,
    public readonly jobs: WorkflowJob[],
  ) {}

  generate(): Schema {
    return { [this.name]: { jobs: this.jobs.map((job) => job.generate()) } };
  }
}
