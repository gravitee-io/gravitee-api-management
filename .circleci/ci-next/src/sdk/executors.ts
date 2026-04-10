/**
 * CircleCI executor factories.
 */

export type DockerResourceClass = 'small' | 'medium' | 'medium+' | 'large' | 'xlarge' | '2xlarge' | '2xlarge+';
export type MachineResourceClass = 'medium' | 'large' | 'xlarge' | '2xlarge';

// Resource class can also be a CircleCI parameter reference like '<< parameters.resource_class >>'
type ResourceClassOrParam<T extends string> = T | (string & {});

export interface DockerExecutor {
  kind: 'docker';
  image: string;
  resourceClass: string;
  environment?: Record<string, string | number | boolean>;
}

export interface MachineExecutor {
  kind: 'machine';
  image: string;
  resourceClass: string;
  dockerLayerCaching?: boolean;
}

export type Executor = DockerExecutor | MachineExecutor;

export function docker(image: string, resourceClass: ResourceClassOrParam<DockerResourceClass> = 'medium', environment?: Record<string, string | number | boolean>): DockerExecutor {
  return { kind: 'docker', image, resourceClass, ...(environment && { environment }) };
}

export function machine(image: string, resourceClass: ResourceClassOrParam<MachineResourceClass> = 'medium', dockerLayerCaching?: boolean): MachineExecutor {
  return { kind: 'machine', image, resourceClass, ...(dockerLayerCaching !== undefined && { dockerLayerCaching }) };
}

export function serializeExecutor(executor: Executor): Record<string, unknown> {
  if (executor.kind === 'docker') {
    const docker: Record<string, unknown> = { image: executor.image };
    if (executor.environment) docker.environment = executor.environment;
    return {
      docker: [docker],
      resource_class: executor.resourceClass,
    };
  }
  return {
    machine: { image: executor.image, docker_layer_caching: executor.dockerLayerCaching ?? false },
    resource_class: executor.resourceClass,
  };
}
