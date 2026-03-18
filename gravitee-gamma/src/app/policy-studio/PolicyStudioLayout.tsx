import { useState, type Dispatch } from 'react';
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  useSensor,
  useSensors,
  type DragStartEvent,
  type DragOverEvent,
  type DragEndEvent,
} from '@dnd-kit/core';
import type { PolicyPlugin, StepKey, Phase, DragData } from './types';
import type { PolicyStudioState, PolicyStudioAction } from './policy-studio.reducer';
import { newStep, isPhaseCompatible } from './policy-studio.utils';
import { FlowsSidebar } from './components/FlowsSidebar';
import { FlowCanvas } from './components/FlowCanvas';
import { PolicyCatalog } from './components/PolicyCatalog';
import { StepConfigSheet } from './components/StepConfigSheet';
import { ErrorBoundary } from './components/ErrorBoundary';

interface PolicyStudioLayoutProps {
  readonly state: PolicyStudioState;
  readonly dispatch: Dispatch<PolicyStudioAction>;
  readonly policies: PolicyPlugin[];
  readonly apiType: string;
}

export function PolicyStudioLayout({ state, dispatch, policies, apiType }: PolicyStudioLayoutProps) {
  const [selectedStepKey, setSelectedStepKey] = useState<StepKey | null>(null);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [activeDragData, setActiveDragData] = useState<DragData | null>(null);
  const [dropState, setDropState] = useState<Record<string, 'compatible' | 'incompatible' | null>>({});

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
  );

  const currentFlow = state.flows[state.selectedFlowIndex] ?? null;
  const selectedStep = selectedStepKey && currentFlow
    ? (currentFlow[selectedStepKey.phase] ?? [])[selectedStepKey.index] ?? null
    : null;

  function handleStepSelect(stepKey: StepKey) {
    setSelectedStepKey(stepKey);
    setSheetOpen(true);
  }

  function handleDragStart(event: DragStartEvent) {
    const data = event.active.data.current as DragData | undefined;
    setActiveDragData(data ?? null);
  }

  function handleDragOver(event: DragOverEvent) {
    const { active, over } = event;
    if (!over) {
      setDropState({});
      return;
    }

    const dragData = active.data.current as DragData | undefined;
    if (!dragData || dragData.type !== 'policy') return;

    const overData = over.data.current as { type?: string; phase?: Phase } | undefined;
    if (overData?.type !== 'phase' || !overData.phase) return;

    const policy = policies.find((p) => p.id === dragData.policyId);
    const compatible = isPhaseCompatible(policy, overData.phase, apiType);

    setDropState({ [over.id as string]: compatible ? 'compatible' : 'incompatible' });
  }

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    setActiveDragData(null);
    setDropState({});

    if (!over) return;

    const dragData = active.data.current as DragData | undefined;
    if (!dragData) return;

    // Drag policy from catalog to a phase
    if (dragData.type === 'policy') {
      const overData = over.data.current as { type?: string; phase?: Phase; flowIndex?: number } | undefined;
      if (overData?.type !== 'phase' || !overData.phase) return;

      const policy = policies.find((p) => p.id === dragData.policyId);
      if (!isPhaseCompatible(policy, overData.phase, apiType)) return;

      const step = newStep(dragData.policyId, policies);
      dispatch({
        type: 'ADD_STEP',
        flowIndex: overData.flowIndex ?? state.selectedFlowIndex,
        phase: overData.phase,
        step,
      });
      return;
    }

    // Reorder step within a phase
    if (dragData.type === 'step') {
      if (active.id === over.id) return;

      const flow = state.flows[dragData.flowIndex];
      if (!flow) return;

      const steps = flow[dragData.phase] ?? [];
      const oldIndex = steps.findIndex((s) => s.id === active.id);
      const newIndex = steps.findIndex((s) => s.id === over.id);

      if (oldIndex === -1 || newIndex === -1) return;

      dispatch({
        type: 'REORDER_STEP',
        flowIndex: dragData.flowIndex,
        phase: dragData.phase,
        from: oldIndex,
        to: newIndex,
      });
    }
  }

  function handleDragCancel() {
    setActiveDragData(null);
    setDropState({});
  }

  function getDropStateForPhase(phase: Phase): 'compatible' | 'incompatible' | null {
    const key = `phase-${state.selectedFlowIndex}-${phase}`;
    return dropState[key] ?? null;
  }

  // Find the dragged item name for the overlay
  const dragOverlayLabel = (() => {
    if (!activeDragData) return null;
    if (activeDragData.type === 'policy') {
      const policy = policies.find((p) => p.id === activeDragData.policyId);
      return policy?.name ?? activeDragData.policyId;
    }
    if (activeDragData.type === 'step') {
      const flow = state.flows[activeDragData.flowIndex];
      const step = flow?.[activeDragData.phase]?.[activeDragData.index];
      return step?.name ?? step?.policy ?? 'Step';
    }
    return null;
  })();

  return (
    <DndContext
      sensors={sensors}
      onDragStart={handleDragStart}
      onDragOver={handleDragOver}
      onDragEnd={handleDragEnd}
      onDragCancel={handleDragCancel}
    >
      <div className="flex h-full overflow-hidden border rounded-lg">
        <ErrorBoundary fallbackLabel="Sidebar error">
          <FlowsSidebar
            flows={state.flows}
            selectedIndex={state.selectedFlowIndex}
            onSelect={(index) => dispatch({ type: 'SELECT_FLOW', index })}
            onAdd={(flow) => dispatch({ type: 'ADD_FLOW', flow })}
            onRemove={(index) => dispatch({ type: 'REMOVE_FLOW', index })}
            onToggle={(index) => dispatch({ type: 'TOGGLE_FLOW_ENABLED', index })}
          />
        </ErrorBoundary>

        <ErrorBoundary fallbackLabel="Canvas error">
          <FlowCanvas
            flow={currentFlow}
            flowIndex={state.selectedFlowIndex}
            selectedStepKey={selectedStepKey}
            onStepSelect={handleStepSelect}
            onStepRemove={(phase, stepIndex) =>
              dispatch({ type: 'REMOVE_STEP', flowIndex: state.selectedFlowIndex, phase, stepIndex })
            }
            requestDropState={getDropStateForPhase('request')}
            responseDropState={getDropStateForPhase('response')}
          />
        </ErrorBoundary>

        <ErrorBoundary fallbackLabel="Catalog error">
          <PolicyCatalog policies={policies} />
        </ErrorBoundary>
      </div>

      <DragOverlay>
        {dragOverlayLabel && (
          <div className="rounded-md border bg-background px-3 py-1.5 text-sm shadow-lg">
            {dragOverlayLabel}
          </div>
        )}
      </DragOverlay>

      <StepConfigSheet
        open={sheetOpen}
        onOpenChange={setSheetOpen}
        step={selectedStep}
        phase={selectedStepKey?.phase ?? null}
        onSave={(configuration) => {
          if (!selectedStepKey) return;
          dispatch({
            type: 'UPDATE_STEP_CONFIG',
            flowIndex: state.selectedFlowIndex,
            phase: selectedStepKey.phase,
            stepIndex: selectedStepKey.index,
            configuration,
          });
        }}
      />
    </DndContext>
  );
}
