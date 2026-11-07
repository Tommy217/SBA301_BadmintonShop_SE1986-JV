import { useState, useMemo } from 'react';
import { Alert, Badge, Button, Card, Nav } from 'react-bootstrap';
import { Link, useLocation } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
import { ticketStatuses, ticketTypes } from '@/modules/support/constants';
import { formatDateTime, formatMoney } from '@/shared/utils/format';

export default function MySupportTicketsPage() {
  const { data } = useShop();
  const { state } = useLocation();
  const [tabFilter, setTabFilter] = useState('all');

  const supportTickets = useMemo(() => {
    return [...(data?.support_tickets || [])].sort(
      (a, b) => new Date(b.created_at) - new Date(a.created_at),
    );
  }, [data?.support_tickets]);

  const filteredTickets = useMemo(() => {
    if (tabFilter === 'in_progress') {
      return supportTickets.filter((t) =>
        ['PENDING', 'open', 'under_review', 'AWAITING_RETURN', 'PENDING_APPROVAL', 'PENDING_TRANSFER'].includes(t.status),
      );
    }
    if (tabFilter === 'completed') {
      return supportTickets.filter((t) => ['REFUNDED', 'COMPLETED', 'resolved'].includes(t.status));
    }
    if (tabFilter === 'closed') {
      return supportTickets.filter((t) => ['REJECTED', 'INSPECTION_FAILED', 'CANCELLED'].includes(t.status));
    }
    return supportTickets;
  }, [supportTickets, tabFilter]);

  return (
    <>
      <div className="eyebrow">HỖ TRỢ KHÁCH HÀNG (UC15)</div>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h1 className="h2 fw-bold mb-1">Phiếu Trả hàng & Hoàn tiền</h1>
          <p className="text-secondary mb-0">
            Theo dõi tiến độ xử lý và kết quả các yêu cầu trả hàng, hoàn tiền của bạn.
          </p>
        </div>
        <Button as={Link} to="/support-tickets/create" variant="primary">
          + Tạo yêu cầu mới
        </Button>
      </div>

      {state?.created && (
        <Alert variant="success" dismissible>
          Yêu cầu hỗ trợ của bạn đã được gửi thành công. Nhân viên sẽ tiến hành sơ duyệt trong vòng 48 giờ.
        </Alert>
      )}

      {/* TABS LỌC TRẠNG THÁI */}
      <Nav variant="tabs" className="mb-4" activeKey={tabFilter} onSelect={(k) => setTabFilter(k)}>
        <Nav.Item>
          <Nav.Link eventKey="all">
            Tất cả ({supportTickets.length})
          </Nav.Link>
        </Nav.Item>
        <Nav.Item>
          <Nav.Link eventKey="in_progress">
            Đang xử lý ({
              supportTickets.filter((t) =>
                ['PENDING', 'open', 'under_review', 'AWAITING_RETURN', 'PENDING_APPROVAL', 'PENDING_TRANSFER'].includes(t.status),
              ).length
            })
          </Nav.Link>
        </Nav.Item>
        <Nav.Item>
          <Nav.Link eventKey="completed">
            Hoàn tất ({
              supportTickets.filter((t) => ['REFUNDED', 'COMPLETED', 'resolved'].includes(t.status)).length
            })
          </Nav.Link>
        </Nav.Item>
        <Nav.Item>
          <Nav.Link eventKey="closed">
            Từ chối / Đã hủy ({
              supportTickets.filter((t) => ['REJECTED', 'INSPECTION_FAILED', 'CANCELLED'].includes(t.status)).length
            })
          </Nav.Link>
        </Nav.Item>
      </Nav>

      {filteredTickets.length === 0 ? (
        <Card className="border-0 p-5 text-center">
          <Card.Body>
            <h2 className="h5 fw-bold mb-2">Chưa có phiếu hỗ trợ nào</h2>
            <p className="text-secondary mb-3">
              {tabFilter === 'all'
                ? 'Bạn chưa tạo phiếu yêu cầu trả hàng hoặc hoàn tiền nào.'
                : 'Không có phiếu nào trong mục này.'}
            </p>
            <Button as={Link} to="/support-tickets/create" variant="outline-primary">
              Gửi yêu cầu ngay
            </Button>
          </Card.Body>
        </Card>
      ) : (
        <div className="d-flex flex-column gap-3">
          {filteredTickets.map((ticket) => {
            const statusConfig = ticketStatuses[ticket.status] || {
              label: ticket.status,
              variant: 'secondary',
            };
            const order = data?.orders?.find((item) => item.id === ticket.order_id);
            const ticketItems = (data?.ticket_items || []).filter((ti) => ti.ticket_id === ticket.id);

            return (
              <Card key={ticket.id} className="border-0 shadow-sm">
                <Card.Body className="p-4">
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
                    <div>
                      <div className="d-flex flex-wrap align-items-center gap-2 mb-2">
                        <span className="fw-bold fs-5">{ticket.ticket_code}</span>
                        <Badge bg="dark">
                          {ticketTypes[ticket.ticket_type] || ticket.ticket_type}
                        </Badge>
                        <Badge bg={statusConfig.variant} text={statusConfig.variant === 'warning' ? 'dark' : undefined}>
                          {statusConfig.label}
                        </Badge>
                      </div>

                      <div className="small text-secondary mb-2">
                        Gửi lúc: {formatDateTime(ticket.created_at)}
                        {order && (
                          <> · Đơn hàng: <strong className="text-dark">{order.order_code}</strong> ({formatMoney(order.total_amount)})</>
                        )}
                        {ticketItems.length > 0 && (
                          <> · Số sản phẩm: <strong>{ticketItems.length}</strong> món</>
                        )}
                      </div>
                    </div>

                    <Button
                      as={Link}
                      to={`/support-tickets/${encodeURIComponent(ticket.id)}`}
                      variant="outline-primary"
                      size="sm"
                    >
                      Chi tiết & Tiến trình →
                    </Button>
                  </div>

                  <hr className="my-2" />

                  <div className="row g-2 align-items-center mt-1">
                    <div className="col-md-8">
                      <div className="small text-secondary">Lý do yêu cầu:</div>
                      <div className="fw-medium text-dark">{ticket.reason}</div>
                    </div>

                    {ticket.refund_amount != null && (
                      <div className="col-md-4 text-md-end">
                        <span className="small text-secondary d-block">Số tiền hoàn:</span>
                        <strong className="fs-6 text-success">{formatMoney(ticket.refund_amount)}</strong>
                      </div>
                    )}
                  </div>

                  {ticket.reject_reason && (
                    <div className="mt-3 p-2 rounded bg-danger-subtle text-danger small">
                      <strong>Lý do từ chối:</strong> {ticket.reject_reason}
                    </div>
                  )}

                  {ticket.refund_record?.bank_reference && (
                    <div className="mt-2 p-2 rounded bg-success-subtle text-success small">
                      <strong>Đã chuyển khoản:</strong> Mã GD ngân hàng {ticket.refund_record.bank_reference}
                    </div>
                  )}
                </Card.Body>
              </Card>
            );
          })}
        </div>
      )}
    </>
  );
}
