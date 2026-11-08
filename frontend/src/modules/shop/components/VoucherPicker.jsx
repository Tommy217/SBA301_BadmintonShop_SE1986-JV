import { useState } from 'react';
import { Alert, Badge, Button, Form, Modal, Spinner } from 'react-bootstrap';
import { calculateVoucher } from '../utils/voucherRules';
import { formatMoney } from '@/shared/utils/format';
import Icon from '@/shared/components/Icon';
import './VoucherPicker.css';

export default function VoucherPicker({
  data,
  items,
  userId,
  appliedCode,
  onSelect,
  onRemove,
  loading,
  error,
  onRetry,
  disabled,
}) {
  const [show, setShow] = useState(false);
  const [filter, setFilter] = useState('all');
  const [search, setSearch] = useState('');
  const vouchers = (data.vouchers || [])
    .map((voucher) => {
      try {
        return {
          voucher,
          discount: calculateVoucher(data, voucher.code, items, userId).discount,
          reason: '',
        };
      } catch (e) {
        return { voucher, discount: 0, reason: e.message };
      }
    })
    .sort((a, b) => Number(!!a.reason) - Number(!!b.reason) || b.discount - a.discount);
  const visible = vouchers.filter(
    ({ voucher }) =>
      (filter === 'all' || (voucher.audience || 'public') === filter) &&
      `${voucher.code} ${voucher.name}`
        .toLocaleLowerCase('vi')
        .includes(search.toLocaleLowerCase('vi')),
  );
  return (
    <div className="checkout-voucher-picker">
      <Button
        type="button"
        variant="outline-primary"
        className="voucher-open"
        onClick={() => setShow(true)}
        disabled={disabled}
      >
        <span>
          <Icon name="receipt" />
          <span>
            <strong>{appliedCode || 'Chọn mã giảm giá'}</strong>
            <small>
              {appliedCode ? 'Đã chọn mã · Bấm để thay đổi' : 'Ưu đãi dành cho đơn hàng của bạn'}
            </small>
          </span>
        </span>
        <span aria-hidden="true">›</span>
      </Button>
      {appliedCode && (
        <Button variant="link" size="sm" type="button" disabled={disabled} onClick={onRemove}>
          Bỏ mã giảm giá
        </Button>
      )}
      <Modal
        show={show}
        onHide={() => setShow(false)}
        centered
        scrollable
        dialogClassName="voucher-picker-modal"
        aria-labelledby="voucher-picker-title"
      >
        <Modal.Header closeButton>
          <div>
            <Modal.Title id="voucher-picker-title">Chọn mã giảm giá</Modal.Title>
            <p className="text-secondary small mb-0 mt-2">
              Mã dùng chung và ưu đãi riêng được cấp cho bạn.
            </p>
          </div>
        </Modal.Header>
        <Modal.Body>
          <Form.Control
            aria-label="Tìm mã giảm giá"
            placeholder="Tìm theo mã hoặc tên ưu đãi"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="mb-3"
          />
          <div className="voucher-filter" role="group" aria-label="Loại voucher">
            {[
              ['all', 'Tất cả'],
              ['public', 'Mã dùng chung'],
              ['private', 'Mã của tôi'],
            ].map(([value, label]) => (
              <Button
                key={value}
                size="sm"
                variant={filter === value ? 'primary' : 'light'}
                aria-pressed={filter === value}
                onClick={() => setFilter(value)}
              >
                {label}
              </Button>
            ))}
          </div>
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" />
              <p className="small mt-3">Đang tải ưu đãi…</p>
            </div>
          ) : error ? (
            <Alert variant="warning">
              {error}
              <Button className="ms-2" size="sm" onClick={onRetry}>
                Thử lại
              </Button>
            </Alert>
          ) : !visible.length ? (
            <div className="voucher-empty">
              <Icon name="receipt" size={36} />
              <h3 className="h6 mt-3">Chưa có mã giảm giá</h3>
              <p>Không có ưu đãi phù hợp với bộ lọc hiện tại.</p>
            </div>
          ) : (
            <div className="voucher-list">
              {visible.map(({ voucher: v, discount, reason }) => (
                <article className={`voucher-ticket ${reason ? 'is-unavailable' : ''}`} key={v.id}>
                  <div className="voucher-value">
                    <strong>
                      {v.discount_type === 'percent'
                        ? `${v.discount_value}%`
                        : formatMoney(v.discount_value)}
                    </strong>
                    <small>GIẢM GIÁ</small>
                  </div>
                  <div className="voucher-copy">
                    <div className="d-flex gap-2 align-items-center flex-wrap">
                      <strong>{v.code}</strong>
                      <Badge
                        bg={v.audience === 'private' ? 'primary' : 'light'}
                        text={v.audience === 'private' ? undefined : 'secondary'}
                      >
                        {v.audience === 'private' ? 'Dành riêng cho bạn' : 'Dùng chung'}
                      </Badge>
                    </div>
                    <h3>{v.name}</h3>
                    <p>
                      Đơn từ {formatMoney(v.min_order)}
                      {v.max_discount != null
                        ? ` · Giảm tối đa ${formatMoney(v.max_discount)}`
                        : ''}
                    </p>
                    {v.description && <p>{v.description}</p>}
                    <p>
                      Áp dụng:{' '}
                      {
                        {
                          all: 'Tất cả sản phẩm',
                          product: 'Sản phẩm được chọn',
                          brand: 'Thương hiệu được chọn',
                          category: 'Danh mục được chọn',
                        }[v.applies_to]
                      }
                    </p>
                    <p>Hạn dùng: {new Date(v.ends_at).toLocaleString('vi-VN')}</p>
                    {reason ? (
                      <p className="voucher-reason">{reason}</p>
                    ) : (
                      <p className="voucher-savings">
                        Tiết kiệm {formatMoney(discount)} cho đơn này
                      </p>
                    )}
                    <Button
                      size="sm"
                      disabled={!!reason || disabled}
                      onClick={() => {
                        onSelect(v.code);
                        setShow(false);
                      }}
                    >
                      {appliedCode === v.code ? 'Đang áp dụng' : 'Sử dụng mã'}
                    </Button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <span className="small text-secondary me-auto">
            Mỗi đơn hàng áp dụng một mã giảm giá.
          </span>
          <Button variant="light" onClick={() => setShow(false)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}
