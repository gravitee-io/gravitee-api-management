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
import * as fs from 'fs';
import { isBlank } from './string';
import { CircleCIEnvironment } from '../pipelines';

export interface GraviteeioVersion {
  full: string;
  version: {
    full: string;
    major: string;
    minor: string;
    patch: string;
  };
  qualifier: {
    full: string;
    name: string;
    version: string;
  };
}

export function parse(graviteeioVersion: string): GraviteeioVersion {
  const [versionFull, qualifierFull] = graviteeioVersion.split('-');

  const [major, minor, patch] = versionFull.split('.');
  const [qualifierName, qualifierVersion] = qualifierFull?.split('.') ?? ['', ''];

  return {
    full: graviteeioVersion,
    version: {
      full: versionFull ?? '',
      major: major ?? '',
      minor: minor ?? '',
      patch: patch ?? '',
    },
    qualifier: {
      full: qualifierFull ?? '',
      name: qualifierName ?? '',
      version: qualifierVersion ?? '',
    },
  };
}

export function computeApimVersion(environment: CircleCIEnvironment): string {
  if (!fs.existsSync(environment.apimVersionPath)) {
    throw new Error('computeApiVersion - No file at specified path: ' + environment.apimVersionPath);
  }
  const pomXml = fs.readFileSync(environment.apimVersionPath, 'utf8');
  const { revision, sha1, changelist } = parsePomXml(pomXml);
  return `${revision}${sha1}${changelist}`;
}

function parsePomXml(pomXml: string) {
  const revisionMatch = pomXml.match(/<revision>(.*?)<\/revision>/);
  const sha1Match = pomXml.match(/<sha1>(.*?)<\/sha1>/);
  const changelistMatch = pomXml.match(/<changelist>(.*?)<\/changelist>/);

  return {
    revision: revisionMatch && revisionMatch.length > 0 ? revisionMatch[1] : '',
    sha1: sha1Match && sha1Match.length > 0 ? sha1Match[1] : '',
    changelist: changelistMatch && changelistMatch.length > 0 ? changelistMatch[1] : '',
  };
}

export function validateGraviteeioVersion(graviteeioVersion: string) {
  if (isBlank(graviteeioVersion)) {
    throw new Error('Graviteeio version is not defined - Please export CI_GRAVITEEIO_VERSION environment variable');
  }
}
