export function getPaymentQrImageUrl(payment) {
  return payment.qrCodeUrl || '';
}
