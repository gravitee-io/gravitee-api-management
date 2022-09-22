import { CustomParameter } from '../../sdk/index.mjs';

export function createDlcParameter(
  defaultValue = false,
  description = `The resource class. Default is ${defaultValue}`,
) {
  return new CustomParameter('docker_layer_caching', 'boolean', defaultValue, description);
}
