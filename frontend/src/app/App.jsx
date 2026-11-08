import { RouterProvider } from 'react-router';
import AuthProvider from '@/app/providers/auth/AuthProvider';
import { router } from '@/app/router/routes';

function redirectToLogin() {
  if (router.state.location.pathname === '/login') return;
  const from = router.state.location;
  router.navigate('/login', {
    replace: true,
    state: { from, message: 'Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại.' },
  });
}

function App() {
  return (
    <AuthProvider onSessionExpired={redirectToLogin}>
      <RouterProvider router={router} />
    </AuthProvider>
  );
}

export default App;
