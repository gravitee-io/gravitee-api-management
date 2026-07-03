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

/**
 * Drop-in internal replacement for `@circleci/circleci-config-sdk`. The public
 * surface (named exports and namespaces) mirrors the original SDK so consumer
 * code only needs to switch its import specifier.
 */
import * as commands from './commands';
import * as executors from './executors';
import * as orb from './orb';
import * as parameters from './parameters';
import * as reusable from './reusable';
import * as workflow from './workflow';

export { Config } from './config';
export { Job } from './job';
export { Workflow } from './workflow';

export { commands, executors, orb, parameters, reusable, workflow };

// Type re-exports matching the SDK's deep (`dist/...`) import paths.
export type { Command, DockerResourceClass, Generable, MachineResourceClass } from './types';
export type { Executor } from './executors';
export type { JobOptionalProperties } from './job';
export type { AnyParameter } from './parameters';
export type { ReusableCommand, ReusedCommand } from './reusable';

/** Phantom literal types kept for API compatibility with the original SDK. */
export type AnyParameterLiteral = unknown;
export type CommandParameterLiteral = unknown;
