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
import { aquasec } from './aquasec';
import { artifactory } from './artifactory';
import { awsCli } from './aws-cli';
import { awsS3 } from './aws-s3';
import { github } from './github';
import { helm } from './helm';
import { keeper } from './keeper';
import { slack } from './slack';
import { snyk } from './snyk';

export const orbs = {
  aquasec,
  artifactory,
  awsCli,
  awsS3,
  github,
  helm,
  keeper,
  slack,
  snyk,
};
