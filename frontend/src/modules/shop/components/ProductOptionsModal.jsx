import { useRef, useState } from 'react';
import { Form, Modal } from 'react-bootstrap';
import { formatMoney } from '@/shared/utils/format';
import { productImage, productImageFallback } from '../utils/productImages';

export default function ProductOptionsModal({ product, data, buy, busy, onHide, onConfirm }) {
  const [color, setColor] = useState('');
  const [size, setSize] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const lock = useRef(false);
  const variants = data.product_variants.filter((v) => String(v.product_id) === String(product.id) && v.is_active);
  const colorOf = (v) => v.color || 'Không phân màu';
  const sizeOf = (v) => v.size || 'Một kích cỡ';
  const colors = [...new Set(variants.map(colorOf))];
  const sizes = [...new Set(variants.map(sizeOf))];
  const available = variants.filter((v) => v.stock_quantity > 0);
  const variant = variants.find((v) => colorOf(v) === color && sizeOf(v) === size);
  const stock = variant?.stock_quantity ?? available.filter((v) => !color || colorOf(v) === color).reduce((sum, v) => sum + v.stock_quantity, 0);
  const price = variant?.price ?? (available.length ? Math.min(...available.map((v) => v.price)) : product.base_price);
  const locked = busy || submitting;
  const maxQuantity = variant?.stock_quantity || 1;
  const selectedQuantity = Math.min(quantity, maxQuantity);
  function chooseColor(value) {
    setColor(value);
    if (!available.some((v) => colorOf(v) === value && sizeOf(v) === size)) setSize('');
  }
  async function confirm(event) {
    event.preventDefault();
    if (!variant || variant.stock_quantity <= 0 || locked || lock.current) return;
    lock.current = true;
    setSubmitting(true);
    try { await onConfirm(variant.id, buy, selectedQuantity); onHide(); }
    catch { /* Provider displays errors; retain the selection for retry. */ }
    finally { lock.current = false; setSubmitting(false); }
  }
  return <Modal show onHide={locked ? undefined : onHide} centered scrollable className="purchase-modal" aria-labelledby="product-options-title">
    <Form onSubmit={confirm}>
      <Modal.Body>
        <button type="button" className="purchase-close" aria-label="Đóng" disabled={locked} onClick={onHide}>×</button>
        <div className="purchase-summary">
          <div className="purchase-photo"><img src={productImage(data, product, variant?.id)} onError={productImageFallback} alt={product.name} /></div>
          <div className="purchase-info">
            <span className="purchase-eyebrow">CHỌN PHÂN LOẠI</span>
            <h2 id="product-options-title">{product.name}</h2>
            <strong className="purchase-price">{formatMoney(price)}</strong>
            <span className="purchase-stock">{stock > 0 ? 'Còn hàng' : 'Tạm hết hàng'}</span>
          </div>
        </div>
        <div className="purchase-selection" aria-live="polite">{color && size ? 'Đã chọn: ' + color + ' / ' + size : 'Vui lòng chọn ' + (!color && !size ? 'màu sắc và kích cỡ' : !color ? 'màu sắc' : 'kích cỡ')}</div>
        <fieldset className="purchase-group" disabled={locked}>
          <legend>Màu sắc {color && <span>{color}</span>}</legend>
          <div className="purchase-options">{colors.map((value) => {
            const enabled = available.some((v) => colorOf(v) === value);
            const imageVariant = variants.find((v) => colorOf(v) === value);
            return <button type="button" key={value} className={'purchase-chip purchase-color' + (color === value ? ' is-selected' : '')} aria-pressed={color === value} disabled={!enabled} onClick={() => chooseColor(value)}>
              <img src={productImage(data, product, imageVariant?.id)} onError={productImageFallback} alt="" /><span>{value}{!enabled && <small>Hết hàng</small>}</span>
            </button>;
          })}</div>
        </fieldset>
        <fieldset className="purchase-group" disabled={locked}>
          <legend>{product.product_type === 'racket' ? 'Trọng lượng / Cỡ cán' : 'Kích cỡ'} {size && <span>{size}</span>}</legend>
          <div className="purchase-options">{sizes.map((value) => {
            const enabled = available.some((v) => sizeOf(v) === value && (!color || colorOf(v) === color));
            return <button type="button" key={value} className={'purchase-chip' + (size === value ? ' is-selected' : '')} aria-pressed={size === value} disabled={!enabled} onClick={() => setSize(value)}>{value}</button>;
          })}</div>
        </fieldset>
        {product.product_type === 'racket' && <p className="purchase-hint">U: nhóm trọng lượng vợt · G: kích cỡ cán vợt</p>}
        <div className="purchase-quantity-row">
          <div><label htmlFor="purchase-quantity">Số lượng</label>{!variant && <small>Chọn phân loại trước</small>}</div>
          <div className="purchase-stepper">
            <button type="button" aria-label="Giảm số lượng" disabled={locked || !variant || selectedQuantity <= 1} onClick={() => setQuantity(selectedQuantity - 1)}>−</button>
            <input id="purchase-quantity" type="number" min="1" max={maxQuantity} step="1" value={selectedQuantity} disabled={locked || !variant} onChange={(event) => setQuantity(Math.min(maxQuantity, Math.max(1, Math.floor(Number(event.target.value) || 1))))} />
            <button type="button" aria-label="Tăng số lượng" disabled={locked || !variant || selectedQuantity >= maxQuantity} onClick={() => setQuantity(selectedQuantity + 1)}>+</button>
          </div>
        </div>
      </Modal.Body>
      <Modal.Footer>
        <div className="purchase-total"><span>Tạm tính · {selectedQuantity} sản phẩm</span><strong>{formatMoney(price * selectedQuantity)}</strong></div>
        <button className="purchase-confirm" type="submit" disabled={locked || !variant || variant.stock_quantity <= 0}>{submitting ? 'Đang xử lý…' : buy ? 'Tiếp tục đặt hàng' : 'Thêm vào giỏ hàng'}</button>
      </Modal.Footer>
    </Form>
  </Modal>;
}
