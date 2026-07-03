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
 * Minimal internal replacement for the (archived) `@circleci/circleci-config-sdk`.
 *
 * Every component knows how to `generate()` the plain object that maps to its
 * CircleCI YAML fragment. `Config.stringify()` assembles these fragments and
 * serializes them with the `yaml` library. The public surface (class names and
 * namespaces) mirrors the original SDK so consumer code only changes its import.
 */

/** Any config component able to emit its CircleCI schema fragment. */
export interface Generable {
  generate(): unknown;
}

/** A YAML object fragment keyed by string. */
export type Schema = Record<string, unknown>;

export type ParameterType = 'string' | 'boolean' | 'integer' | 'enum';

/** Resource classes accepted by a Docker executor. */
export type DockerResourceClass = 'small' | 'medium' | 'medium+' | 'large' | 'xlarge' | '2xlarge' | '2xlarge+';

/** Resource classes accepted by a Machine executor. */
export type MachineResourceClass = 'medium' | 'large' | 'xlarge' | '2xlarge';
