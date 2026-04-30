import { AppRouter } from './router/AppRouter';
import { AppProvider } from './state/AppContext';

export function App() {
  return (
    <AppProvider>
      <AppRouter />
    </AppProvider>
  );
}
