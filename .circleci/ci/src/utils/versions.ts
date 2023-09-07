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
  const pomXml = fs.readFileSync(
    !environment.apimVersionPath ? '~/projects/gravitee-api-management/pom.xml' : environment.apimVersionPath,
    'utf8',
  );
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
