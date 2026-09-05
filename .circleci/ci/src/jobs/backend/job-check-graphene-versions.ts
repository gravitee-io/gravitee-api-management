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
import { Command, Job, commands } from '../../circleci-config';
import { NodeLtsExecutor } from '../../executors';

/**
 * Compares the graphene versions the bundled Gamma modules register into the Module Federation
 * shared scope against the versions APIM pins. Nothing else in the pipeline loads the console host
 * and a remote together, so nothing else sees this drift at all.
 *
 * Reads the module zips out of the distribution 'Build backend' assembled and persisted, so it
 * needs no download, credential or registry access of its own — hence the `requires` on that job.
 */
export class CheckGrapheneVersionsJob {
  public static create(): Job {
    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new commands.Run({
        name: 'Check graphene versions across bundled Gamma modules',
        command: 'npm ci && npx ts-node src/graphene-versions/check.ts',
        working_directory: '.circleci/ci',
      }),
    ];
    return new Job('job-check-graphene-versions', NodeLtsExecutor.create('small'), steps);
  }
}
