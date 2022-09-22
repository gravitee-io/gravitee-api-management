import { CustomEnumParameter } from '../../sdk/index.mjs';

export function createClassParameter(defaultValue, description = `The resource class. Default is ${defaultValue}`) {
  return new CustomEnumParameter('class', ['small', 'medium', 'medium+', 'large', 'xlarge'], defaultValue, description);
}
