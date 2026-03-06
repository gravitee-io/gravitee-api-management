/**
 * Pre-configured executors for Gravitee APIM CI.
 */
import { docker, machine, type DockerResourceClass } from '../sdk/executors.js';
import { executorImages } from '../config/config.js';

const img = (e: { image: string; version: string }) => `${e.image}:${e.version}`;

export const baseExecutor = (size: string = 'medium') => docker(img(executorImages.base), size);

export const openjdkExecutor = (size: string = 'medium') => docker(img(executorImages.openjdk), size);

export const nodeExecutor = (size: string = 'medium') => docker(img(executorImages.node), size);

export const sonarExecutor = (size: string = 'large') => docker(img(executorImages.sonar), size);

export const ubuntuExecutor = () =>
  machine(`ubuntu-${executorImages.ubuntu.version}:${executorImages.ubuntu.tag}`, 'medium');
