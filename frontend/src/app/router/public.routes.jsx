import ProtectedRoute from './ProtectedRoute';
import PublicLayout from '@/app/layouts/public/PublicLayout';
import Login from '@/modules/auth/pages/LoginPage';
import Register from '@/modules/auth/pages/RegisterPage';
import ForgotPassword from '@/modules/auth/pages/ForgotPasswordPage';
import ResetPassword from '@/modules/auth/pages/ResetPasswordPage';
import Checkout from '@/modules/payment/pages/CheckoutPage';
import QrPayment from '@/modules/payment/pages/QrPaymentPage';
import PaymentResult from '@/modules/payment/pages/PaymentResultPage';
import Home from '@/modules/shop/pages/HomePage';
import ProductDetailPage from '@/modules/shop/pages/ProductDetailPage';

import CartPage from '@/modules/shop/pages/CartPage';
import OrderPage from '@/modules/shop/pages/OrderPage';
import OrdersPage from '@/modules/shop/pages/OrdersPage';
import MySupportTicketsPage from '@/modules/support/pages/MySupportTicketsPage';
import CreateSupportTicketPage from '@/modules/support/pages/CreateSupportTicketPage';
import SupportTicketDetailPage from '@/modules/support/pages/SupportTicketDetailPage';
import OrderTrackingPage from '@/modules/delivery/pages/OrderTrackingPage';
import ProfilePage from '@/modules/auth/pages/ProfilePage';
import { Navigate } from 'react-router';
export const publicRoutes = [
  { path: '/login', element: <Login /> },
  { path: '/register', element: <Register /> },
  { path: '/forgot-password', element: <ForgotPassword /> },
  { path: '/reset-password', element: <ResetPassword /> },
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'products', element: <Home catalog /> },
      { path: 'products/:productId', element: <ProductDetailPage /> },
      {
        element: <ProtectedRoute />,
        children: [
          { path: 'profile', element: <ProfilePage /> },
          { path: 'cart', element: <CartPage /> },
          { path: 'order', element: <OrderPage /> },
          { path: 'orders', element: <OrdersPage /> },
          { path: 'orders/:orderId/tracking', element: <OrderTrackingPage /> },
          { path: 'support-tickets', element: <MySupportTicketsPage /> },
          { path: 'support-tickets/create', element: <CreateSupportTicketPage /> },
          { path: 'support-tickets/:ticketId', element: <SupportTicketDetailPage /> },
          { path: 'my-requests', element: <Navigate to="/support-tickets" replace /> },
          {
            path: 'my-requests/create',
            element: <Navigate to="/support-tickets/create" replace />,
          },
          { path: '/payments', element: <Checkout /> },
          { path: '/payments/qr', element: <QrPayment /> },
          { path: '/payments/result', element: <PaymentResult /> },
        ],
      },
      {
        path: '*',
        element: (
          <div className="empty-state">
            <h1 className="h4">Không tìm thấy trang</h1>
            <a href="/">Về trang chủ</a>
          </div>
        ),
      },
    ],
  },
];
