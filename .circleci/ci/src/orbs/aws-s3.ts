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
import { orb, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const awsS3 = new orb.OrbImport(
  'aws-s3',
  'circleci',
  'aws-s3',
  config.orbs.awsS3,
  'Syncs directories and S3 prefixes.\nhttps://docs.aws.amazon.com/cli/latest/reference/s3/sync.html',
  {
    jobs: {},
    executors: {},
    commands: {
      sync: new parameters.CustomParametersList([
        new parameters.CustomParameter(
          'arguments',
          'string',
          '',
          'Optional additional arguments to pass to the `aws sync` command (e.g. `--acl public-read`). Note: if passing a multi-line value to this parameter, include `\\` characters after each line, so the Bash shell can correctly interpret the entire command.',
        ),
        new parameters.CustomParameter('from', 'string'),
        new parameters.CustomParameter('to', 'string'),
      ]),
    },
  },
);
