import { CustomParameter } from '../../sdk/index.mjs';

export function createVersionParameter(defaultValue, description = `The version. Default is ${defaultValue}`) {
  return new CustomParameter('version', 'string', defaultValue, description);
}
