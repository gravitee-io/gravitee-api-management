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

/**
 * The version a branch goes back to developing once `graviteeioVersion` is released.
 *
 * A final release opens the next patch; a qualified one keeps its number and increments the
 * qualifier, so `4.12.17-hotfix.1` is followed by `4.12.17-hotfix.2` rather than by a patch nobody
 * asked for.
 */
export function nextDevelopmentVersion(graviteeioVersion: string): { version: string; qualifier: string } {
  const parsed = parse(graviteeioVersion);
  const { major, minor, patch } = parsed.version;

  if (parsed.qualifier.full === '') {
    return { version: `${major}.${minor}.${Number(patch) + 1}`, qualifier: '' };
  }
  return {
    version: `${major}.${minor}.${patch}`,
    qualifier: `-${parsed.qualifier.name}.${Number(parsed.qualifier.version) + 1}`,
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

/** A released version with no qualifier. Prefixed tags — the core lane's — never match. */
const FINAL_VERSION = /^\d+\.\d+\.\d+$/;

/**
 * Whether `version` is the newest final release the repository has produced.
 *
 * This replaces the `--latest` flag. A distribution release is started by pushing a tag, and a
 * tag-triggered pipeline receives no parameter, so which images take `latest` cannot be answered at
 * the keyboard any more — it is read from what has already been released. That is also one fewer
 * thing to get wrong: the flag was answered from memory, and a release from an older support line
 * only had a prompt standing between it and overwriting `latest`.
 *
 * A qualified version is never the latest: an alpha must not become what `docker pull` hands to
 * someone who names no tag.
 * @param version the version being released
 * @param tags every tag the repository carries, raw
 */
export function isLatestRelease(version: string, tags: string[]): boolean {
  if (!FINAL_VERSION.test(version)) {
    return false;
  }

  const rank = (v: string) => v.split('.').map(Number);
  const [major, minor, patch] = rank(version);
  const isHigher = (candidate: string) => {
    const [otherMajor, otherMinor, otherPatch] = rank(candidate);
    if (otherMajor !== major) return otherMajor > major;
    if (otherMinor !== minor) return otherMinor > minor;
    return otherPatch > patch;
  };

  return !tags.filter((tag) => FINAL_VERSION.test(tag)).some(isHigher);
}
