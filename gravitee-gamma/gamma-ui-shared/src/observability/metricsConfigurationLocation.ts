/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Where an environment's additional metrics are declared and edited: the Platform module's Metrics configuration
 * page, named as the shell's module switcher and the Platform navigation show it. Tests in the shell and in the
 * Platform module pin each part to the label it renders.
 */
export const METRICS_CONFIGURATION_LOCATION = {
    moduleId: 'platform',
    module: 'Platform Management',
    section: 'Environment',
    group: 'System & Security',
    page: 'Metrics configuration',
} as const;

const { module, section, group, page } = METRICS_CONFIGURATION_LOCATION;

/** The location as a path a reader follows: "Platform Management › Environment › System & Security › Metrics configuration". */
export const METRICS_CONFIGURATION_PATH = [module, section, group, page].join(' › ');
