import VoucherRecipients from './VoucherRecipients';
import { Alert, Badge, Button, Nav } from 'react-bootstrap';
import { useSearchParams } from 'react-router';
import { useCatalog } from './CatalogContext';
import { formatMoney } from '@/shared/utils/format';
import CatalogSection from './CatalogSection';
import { optionLabels } from '@/modules/management/utils/managementUtils';
import { voucherSections } from '@/modules/management/utils/catalogNavigation';

export default function VoucherManagementPage() {
  const { data } = useCatalog();
  const [params, setParams] = useSearchParams();
  const id = params.get('voucher');
  const voucher = (data.vouchers || []).find((v) => String(v.id) === id);
  const tab = Object.hasOwn(voucherSections, params.get('tab')) ? params.get('tab') : 'info';
  function select(row) {
    setParams({ voucher: row.id, tab: params.get('tab') || 'info' });
  }
  if (!voucher)
    return (
      <>
        {id && (
          <Alert variant="warning">
            Voucher không còn tồn tại. Chọn voucher khác trong danh sách.
          </Alert>
        )}
        {params.get('tab') && (
          <Alert variant="info">
            Chọn “Quản lý chi tiết” trên voucher để xem phạm vi và lượt sử dụng của voucher đó.
          </Alert>
        )}
        <CatalogSection
          catalog="vouchers"
          onManage={select}
          onSaved={(result, savedId) => {
            if (!savedId && result.saved) select(result.saved);
          }}
        />
      </>
    );
  const scopes = (data.voucher_scopes || []).filter((s) => String(s.voucher_id) === id);
  const usages = (data.voucher_usages || []).filter((u) => String(u.voucher_id) === id);
  return (
    <section className="manager-page">
      <div className="manager-title">
        <div>
          <div className="eyebrow">QUẢN LÝ VOUCHER</div>
          <h1>{voucher.name}</h1>
          <p>
            {voucher.code} · {optionLabels[voucher.applies_to]} ·{' '}
            {optionLabels[voucher.audience || 'public']}
          </p>
        </div>
        <Button className="app-back-button" variant="outline-primary" onClick={() => setParams({})}>
          ← Danh sách voucher
        </Button>
      </div>
      <div className="manager-entity-summary">
        <div className="manager-voucher-mark">%</div>
        <div>
          <Badge bg={voucher.is_active ? 'success' : 'secondary'}>
            {voucher.is_active ? 'Đang hoạt động' : 'Tạm ngưng'}
          </Badge>
          <strong>
            {voucher.discount_type === 'percent'
              ? voucher.discount_value + '%'
              : formatMoney(voucher.discount_value)}
          </strong>
          <p>
            {voucher.used_count} / {voucher.usage_limit ?? 'Không giới hạn'} lượt · Đã giảm:{' '}
            {formatMoney(usages.reduce((sum, u) => sum + Number(u.discount_amount), 0))}
          </p>
        </div>
      </div>
      {voucher.audience === 'private' && <VoucherRecipients key={id} voucherId={id} />}
      <Nav variant="tabs" className="manager-entity-tabs" aria-label="Các phần quản lý voucher">
        {Object.entries(voucherSections).map(([key, label]) => (
          <Nav.Item key={key}>
            <Nav.Link
              as="button"
              type="button"
              active={tab === key}
              onClick={() => setParams({ voucher: id, tab: key })}
            >
              {label}
              {key !== 'info' && (
                <Badge bg="light" text="secondary" className="ms-2">
                  {key === 'scopes' ? scopes.length : usages.length}
                </Badge>
              )}
            </Nav.Link>
          </Nav.Item>
        ))}
      </Nav>
      {tab === 'info' && (
        <CatalogSection
          key={id + '-info'}
          catalog="vouchers"
          scope={{ id: voucher.id }}
          embedded
          allowCreate={false}
        />
      )}
      {tab === 'scopes' &&
        (voucher.applies_to === 'all' ? (
          <Alert variant="info">
            Voucher áp dụng cho toàn bộ sản phẩm. Không cần chọn phạm vi riêng. Bạn có thể thay đổi
            cách áp dụng trong tab Thông tin voucher.
          </Alert>
        ) : (
          <>
            <p className="small text-secondary">
              Chọn {optionLabels[voucher.applies_to].toLowerCase()} được áp dụng. Mỗi phạm vi mới tự
              gắn với voucher {voucher.code}.
            </p>
            <CatalogSection
              key={id + '-scopes-' + voucher.applies_to}
              catalog="voucher_scopes"
              scope={{ voucher_id: voucher.id, scope_type: voucher.applies_to }}
              embedded
            />
          </>
        ))}
      {tab === 'usages' && (
        <CatalogSection
          key={id + '-usages'}
          catalog="voucher_usages"
          scope={{ voucher_id: voucher.id }}
          embedded
          allowCreate={false}
        />
      )}
    </section>
  );
}
