import { useReducer, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { PageLayout } from '@baros/components/layout/PageLayout';
import { Button } from '@baros/components/ui/button';
import { Save } from 'lucide-react';
import { useApi } from './hooks/useApi';
import { usePolicies } from './hooks/usePolicies';
import { policyStudioReducer, initialState } from './policy-studio.reducer';
import { safeFlows } from './policy-studio.utils';
import { PolicyStudioLayout } from './PolicyStudioLayout';

export function PolicyStudioPage() {
  const { apiId } = useParams<{ apiId: string }>();
  const { api, loading: apiLoading, error: apiError, saveApi } = useApi(apiId ?? '');
  const { policies, loading: policiesLoading, error: policiesError } = usePolicies();
  const [state, dispatch] = useReducer(policyStudioReducer, initialState);
  const [initializedApiId, setInitializedApiId] = useState<string | null>(null);

  useEffect(() => {
    if (api && api.id !== initializedApiId) {
      dispatch({ type: 'SET_FLOWS', flows: safeFlows(api.flows) });
      setInitializedApiId(api.id);
    }
  }, [api, initializedApiId]);

  const loading = apiLoading || policiesLoading;
  const error = apiError ?? policiesError;

  async function handleSave() {
    if (!api) return;
    dispatch({ type: 'SAVE_START' });
    try {
      await saveApi({ ...api, flows: state.flows });
      dispatch({ type: 'SAVE_DONE' });
    } catch (err) {
      console.error('Save failed:', err);
      dispatch({ type: 'SAVE_ERROR' });
    }
  }

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-muted-foreground">Loading Policy Studio...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-4">
        <div className="text-destructive">Failed to load: {error.message}</div>
        <Button variant="outline" onClick={() => window.location.reload()}>
          Retry
        </Button>
      </div>
    );
  }

  return (
    <div className="flex h-full w-full flex-col overflow-hidden p-4">
      <PageLayout
        title={api?.name ?? 'Policy Studio'}
        description="Configure request and response policies"
        className="shrink-0"
        actions={
          <Button
            onClick={handleSave}
            disabled={!state.isDirty || state.saving}
            iconLeft={<Save />}
          >
            {state.saving ? 'Saving...' : 'Save'}
          </Button>
        }
      />
      <div className="min-h-0 flex-1 pt-4">
        <PolicyStudioLayout
          state={state}
          dispatch={dispatch}
          policies={policies}
          apiType={api?.type ?? 'PROXY'}
        />
      </div>
    </div>
  );
}
