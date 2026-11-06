import { useEffect, useRef, useState } from 'react';
import { Button } from 'react-bootstrap';
import './ImagePicker.css';

export default function ImagePicker({ value, onChange, disabled, required, onBusyChange }) {
  const input = useRef(null);
  const reader = useRef(null);
  const [error, setError] = useState('');
  const [reading, setReading] = useState(false);
  useEffect(() => () => reader.current?.abort(), []);

  function select(event) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setError('');
    if (!['image/png', 'image/jpeg', 'image/gif', 'image/webp'].includes(file.type)) {
      setError('Vui lòng chọn ảnh PNG, JPG, GIF hoặc WebP.');
      return;
    }
    if (!file.size || file.size > 5 * 1024 * 1024) {
      setError('Ảnh phải có dung lượng lớn hơn 0 và tối đa 5 MB.');
      return;
    }
    const next = new FileReader();
    reader.current = next;
    setReading(true);
    onBusyChange(true);
    next.onload = () => onChange(next.result);
    next.onerror = () => setError('Không đọc được ảnh. Vui lòng chọn lại.');
    next.onloadend = () => {
      setReading(false);
      onBusyChange(false);
    };
    next.readAsDataURL(file);
  }

  return (
    <div>
      <input
        ref={input}
        type="file"
        accept="image/png,image/jpeg,image/gif,image/webp"
        hidden
        onChange={select}
        disabled={disabled || reading}
      />
      <button
        type="button"
        className="catalog-image-picker"
        disabled={disabled || reading}
        onClick={() => input.current.click()}
        aria-label={value ? 'Thay ảnh từ máy tính' : 'Chọn ảnh từ máy tính'}
      >
        {value && <img src={value} alt="Ảnh đã chọn" />}
        <span className="catalog-image-plus" aria-hidden="true">
          +
        </span>
        <span>{reading ? 'Đang đọc ảnh…' : value ? 'Thay ảnh' : 'Chọn ảnh từ máy tính'}</span>
      </button>
      <small className="d-block text-secondary mt-2">PNG, JPG, GIF, WebP · Tối đa 5 MB</small>
      {value && !required && (
        <Button
          type="button"
          variant="link"
          size="sm"
          disabled={disabled || reading}
          onClick={() => onChange('')}
        >
          Xóa ảnh
        </Button>
      )}
      {error && (
        <div role="alert" className="text-danger small mt-2">
          {error}
        </div>
      )}
    </div>
  );
}
