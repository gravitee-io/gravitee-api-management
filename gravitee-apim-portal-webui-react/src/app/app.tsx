import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Route, Routes } from 'react-router-dom';
import { ViewPage } from '../pages/ViewPage';
import { EditPage } from '../pages/EditPage';
import { ApiDocPage } from '../pages/ApiDocPage';
import './app.module.scss';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Routes>
        <Route path="/" element={<ViewPage />} />
        <Route path="/edit" element={<EditPage />} />
        <Route path="/api/:apiId" element={<ApiDocPage />} />
      </Routes>
    </QueryClientProvider>
  );
}

export default App;
