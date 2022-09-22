import { CustomParameter } from '../../sdk/index.mjs';

export function createTagParameter(defaultValue, description = `The  tag. Default is ${defaultValue}`) {
  return new CustomParameter('tag', 'string', defaultValue, description);
}
