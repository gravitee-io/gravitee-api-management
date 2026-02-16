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
        groupNumber: 1,
      },
      {
        label: 'Step 2',
        groupNumber: 2,
        menuItemComponent: 'Step2MenuItem' as any,
      },
      {
        label: 'Step 3',
        groupNumber: 3,
      },
      {
        label: 'Step 4',
        groupNumber: 4,
      },
    ],
    {
      selectedEndpoints: [{ id: '1', name: 'initial value', icon: 'gio:language', deployed: true }],
    },
  );

  let currentStep: ApiCreationStep;

  apiCreationStepperService.currentStep$.subscribe(step => {
    currentStep = step;
  });

  it('should got to fist step', () => {
    apiCreationStepperService.goToNextStep({
      groupNumber: 1,
      component: 'Step1Component' as any,
    });

    expect(currentStep.id).toEqual('step-1-1');
    expect(currentStep.group.label).toEqual('Step 1');
  });

  it('should save step(1) and go to next step(2-1)', () => {
    apiCreationStepperService.validStep(previousPayload => ({ ...previousPayload, name: 'Step 1' }));
    apiCreationStepperService.goToNextStep({
      groupNumber: 2,
      component: 'Step2-1Component' as any,
    });

    expect(currentStep.id).toEqual('step-2-1');
    expect(currentStep.group.label).toEqual('Step 2');
  });

  it('should save step(2-1) and go to next step(2-2)', done => {
    apiCreationStepperService.validStep(previousPayload => ({ ...previousPayload, name: `${previousPayload.name} > Step 2.1` }));

    apiCreationStepperService.goToNextStep({
      groupNumber: 2,
      component: 'Step2-2Component' as any,
    });

    apiCreationStepperService.steps$.subscribe(steps => {
      expect(steps.length).toEqual(3);
      expect(steps.find(step => step.id === 'step-2-2')).toEqual({
        id: 'step-2-2',
        component: 'Step2-2Component',
        group: {
          groupNumber: 2,
          label: 'Step 2',
          menuItemComponent: 'Step2MenuItem',
        },
        patchPayload: expect.any(Function),
        state: 'initial',
      });
      done();
    });
  });

  it('should save step(2-2) and go to next step(3)', () => {
    apiCreationStepperService.validStep(previousPayload => ({ ...previousPayload, name: `${previousPayload.name} > Step 2.2` }));

    apiCreationStepperService.goToNextStep({
      groupNumber: 3,
      component: 'Step3Component' as any,
    });

    expect(currentStep.id).toEqual('step-3-1');
    expect(currentStep.group.label).toEqual('Step 3');
  });

  it('should save and go to next step(4)', () => {
    apiCreationStepperService.validStep(previousPayload => ({ ...previousPayload, name: `${previousPayload.name} > Step 3` }));

    apiCreationStepperService.goToNextStep({
      groupNumber: 4,
      component: 'Step4Component' as any,
    });

    expect(currentStep.id).toEqual('step-4-1');
    expect(currentStep.group.label).toEqual('Step 4');
  });

  it('should have compiled payload from step 1 2 3', () => {
    expect(apiCreationStepperService.compileStepPayload(currentStep)).toEqual({
      name: 'Step 1 > Step 2.1 > Step 2.2 > Step 3',
      selectedEndpoints: [{ id: '1', name: 'initial value', icon: 'gio:language', deployed: true }],
    });
  });

  it('should go to step 1 and change patch payload', () => {
    apiCreationStepperService.goToStepLabel('Step 1');
    apiCreationStepperService.validStep(previousPayload => {
      previousPayload.selectedEndpoints.push({ id: '2', name: 'new value', icon: 'gio:language', deployed: true });

      return { ...previousPayload, name: 'Step 1 - edited' };
    });
  });

  it('should re add secondary step at step 2', done => {
    apiCreationStepperService.goToNextStep({
      groupNumber: 2,
      component: 'Step2-1Component' as any,
    });

    apiCreationStepperService.steps$.subscribe(steps => {
      expect(steps.length).toEqual(5);
      expect(steps.find(step => step.id === 'step-2-2')).toBeDefined();
      done();
    });
  });

  it('should go to step 4', () => {
    apiCreationStepperService.goToStepLabel('Step 4');
  });

  it('should have compiled payload from step 1 2 3', () => {
    expect(apiCreationStepperService.compileStepPayload(currentStep)).toEqual({
      name: 'Step 1 - edited > Step 2.1 > Step 2.2 > Step 3',
      selectedEndpoints: [
        { id: '1', name: 'initial value', icon: 'gio:language', deployed: true },
        { id: '2', name: 'new value', icon: 'gio:language', deployed: true },
      ],
    });
  });

  it('should finish stepper', done => {
    apiCreationStepperService.finished$.subscribe(payload => {
      expect(payload).toEqual({
        selectedEndpoints: [
          { id: '1', name: 'initial value', icon: 'gio:language', deployed: true },
          { id: '2', name: 'new value', icon: 'gio:language', deployed: true },
        ],
        name: 'Step 1 - edited > Step 2.1 > Step 2.2 > Step 3',
      });
      done();
    });
    apiCreationStepperService.finishStepper();
  });

  it('should remove all next steps', done => {
    const stepperService = new ApiCreationStepperService(
      [
        {
          label: 'Step 1',
          groupNumber: 1,
        },
        {
          label: 'Step 2',
          groupNumber: 2,
          menuItemComponent: 'Step2MenuItem' as any,
        },
      ],
      {},
    );
    stepperService.goToNextStep({ groupNumber: 1, component: 'Step 1' as any });
    stepperService.goToNextStep({ groupNumber: 2, component: 'Step 2.1' as any });
    stepperService.goToNextStep({ groupNumber: 2, component: 'Step 2.2' as any });
    stepperService.goToStepLabel('Step 1');
    stepperService.removeAllNextSteps();
    stepperService.steps$.subscribe(steps => {
      expect(steps.length).toEqual(1);
      expect(steps).toEqual([
        {
          id: 'step-1-1',
          component: 'Step 1',
          group: {
            groupNumber: 1,
            label: 'Step 1',
          },
          patchPayload: expect.any(Function),
          state: 'initial',
        },
      ]);
      done();
    });
    apiCreationStepperService.removeAllNextSteps();
  });
});
