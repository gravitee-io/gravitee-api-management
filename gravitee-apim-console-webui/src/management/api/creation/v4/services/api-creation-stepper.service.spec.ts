import { ApiCreationStep, ApiCreationStepperService } from './api-creation-stepper.service';

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
describe('ApiCreationStepperService', () => {
  const apiCreationStepperService = new ApiCreationStepperService(
    [
      {
        label: 'Step 1',
        component: undefined,
      },
      {
        label: 'Step 2',
        component: undefined,
        menuItemComponent: 'Step2MenuItem' as any,
      },
      {
        label: 'Step 3',
        component: undefined,
      },
      {
        label: 'Step 4',
        component: undefined,
      },
    ],
    {
      selectedEndpoints: [{ id: '1', name: 'initial value' }],
    },
  );

  let currentStep: ApiCreationStep;

  apiCreationStepperService.currentStep$.subscribe((step) => {
    currentStep = step;
  });

  it('should got to fist step', () => {
    apiCreationStepperService.goToStepLabel('Step 1');

    expect(currentStep.id).toEqual('step-1-1');
    expect(currentStep.label).toEqual('Step 1');
  });

  it('should save and go to next step(2-1)', () => {
    apiCreationStepperService.validStepAndGoNext((previousPayload) => ({ ...previousPayload, name: 'Step 1' }));

    expect(currentStep.id).toEqual('step-2-1');
    expect(currentStep.label).toEqual('Step 2');
  });

  it('should add secondary step ', (done) => {
    apiCreationStepperService.addSecondaryStep({
      component: undefined,
    });

    apiCreationStepperService.steps$.subscribe((steps) => {
      expect(steps.length).toEqual(5);
      expect(steps.find((step) => step.id === 'step-2-2')).toEqual({
        id: 'step-2-2',
        label: 'Step 2',
        component: undefined,
        labelNumber: 2,
        menuItemComponent: 'Step2MenuItem',
        patchPayload: expect.any(Function),
        state: 'initial',
      });
      done();
    });
  });

  it('should save and go to next step(2-2)', () => {
    apiCreationStepperService.validStepAndGoNext((previousPayload) => ({ ...previousPayload, name: `${previousPayload.name} > Step 2` }));

    expect(currentStep.id).toEqual('step-2-2');
    expect(currentStep.label).toEqual('Step 2');
  });

  it('should save and go to next step(3)', () => {
    apiCreationStepperService.validStepAndGoNext((previousPayload) => ({ ...previousPayload, name: `${previousPayload.name} > Step 2.1` }));

    expect(currentStep.id).toEqual('step-3-1');
    expect(currentStep.label).toEqual('Step 3');
  });

  it('should save and go to next step(4)', () => {
    apiCreationStepperService.validStepAndGoNext((previousPayload) => ({ ...previousPayload, name: `${previousPayload.name} > Step 3` }));

    expect(currentStep.id).toEqual('step-4-1');
    expect(currentStep.label).toEqual('Step 4');
  });

  it('should have compiled payload from step 1 2 3', () => {
    expect(apiCreationStepperService.compileStepPayload(currentStep)).toEqual({
      name: 'Step 1 > Step 2 > Step 2.1 > Step 3',
      selectedEndpoints: [{ id: '1', name: 'initial value' }],
    });
  });

  it('should go to step 1 and change patch payload', () => {
    apiCreationStepperService.goToStepLabel('Step 1');
    apiCreationStepperService.validStepAndGoNext((previousPayload) => {
      previousPayload.selectedEndpoints.push({ id: '2', name: 'new value' });

      return { ...previousPayload, name: 'Step 1 - edited' };
    });
  });

  it('should re add secondary step at step 2', (done) => {
    apiCreationStepperService.addSecondaryStep({
      component: undefined,
    });

    apiCreationStepperService.steps$.subscribe((steps) => {
      expect(steps.length).toEqual(5);
      expect(steps.find((step) => step.id === 'step-2-2')).toBeDefined();
      done();
    });
  });

  it('should go to step 4', () => {
    apiCreationStepperService.goToStepLabel('Step 4');
  });

  it('should have compiled payload from step 1 2 3', () => {
    expect(apiCreationStepperService.compileStepPayload(currentStep)).toEqual({
      name: 'Step 1 - edited > Step 2 > Step 2.1 > Step 3',
      selectedEndpoints: [
        { id: '1', name: 'initial value' },
        { id: '2', name: 'new value' },
      ],
    });
  });
});
