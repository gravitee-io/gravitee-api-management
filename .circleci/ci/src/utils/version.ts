export interface GraviteeioVersion {
  version: {
    full?: string;
    major?: string;
    minor?: string;
    patch?: string;
  };
  qualifier: {
    full?: string;
    name?: string;
    version?: string;
  };
}

export function parse(graviteeioVersion: string): GraviteeioVersion {
  const [versionFull, qualifierFull] = graviteeioVersion.split('-');

  const [major, minor, patch] = versionFull.split('.');
  const [name, qualifierVersion] = qualifierFull?.split('.') ?? ['', ''];

  return {
    version: {
      full: versionFull,
      major,
      minor,
      patch,
    },
    qualifier: {
      full: qualifierFull ?? '',
      name,
      version: qualifierVersion,
    },
  };
}
