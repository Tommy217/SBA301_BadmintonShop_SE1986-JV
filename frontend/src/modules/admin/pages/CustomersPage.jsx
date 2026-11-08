import { useState, useEffect, useCallback } from 'react';
import { Table, Badge, Button, Form, Alert, Spinner } from 'react-bootstrap';
import { customerApi } from '@/modules/admin/api/admin.api';

export default function CustomersPage() {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  // Filters
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await customerApi.getCustomers({ size: 1000 });
      const customerList = Array.isArray(response) ? response : response?.content || [];
      setCustomers(customerList);
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách khách hàng.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Statistics
  const stats = {
    total: customers.length,
    active: customers.filter((u) => u.status !== 'locked' && !u.locked).length,
    locked: customers.filter((u) => u.status === 'locked' || u.locked).length,
  };

  // Filtered customers
  const filtered = customers.filter((u) => {
    const matchSearch =
      search.trim() === '' ||
      String(u.fullName || '').toLowerCase().includes(search.toLowerCase()) ||
      String(u.email || '').toLowerCase().includes(search.toLowerCase());

    const isLocked = u.status === 'locked' || Boolean(u.locked);
    const matchStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'active' && !isLocked) ||
      (statusFilter === 'locked' && isLocked);

    return matchSearch && matchStatus;
  });

  // Pagination
  const totalPages = Math.ceil(filtered.length / pageSize) || 1;
  const validPage = Math.min(Math.max(1, currentPage), totalPages);
  const startIndex = (validPage - 1) * pageSize;
  const paginated = filtered.slice(startIndex, startIndex + pageSize);

  const handleToggleStatus = async (u) => {
    const isCurrentlyLocked = u.status === 'locked' || Boolean(u.locked);
    const newStatus = isCurrentlyLocked ? 'active' : 'locked';

    try {
      await customerApi.updateCustomerStatus(u.id, newStatus);
      setNotice(`Đã ${newStatus === 'active' ? 'mở khóa' : 'khóa'} tài khoản khách hàng ${u.email}.`);
      await loadData();
    } catch (err) {
      setError(err.message || 'Không thể cập nhật trạng thái khách hàng.');
    }
  };

  const styles = {
    header: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      marginBottom: '24px',
    },
    title: {
      fontSize: '24px',
      fontWeight: '700',
      color: '#0f172a',
      marginBottom: '4px',
    },
    subtitle: {
      fontSize: '14px',
      color: '#64748b',
    },
    statGrid: {
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
      gap: '16px',
      marginBottom: '24px',
    },
    statCard: {
      backgroundColor: '#ffffff',
      border: '1px solid #e2e8f0',
      borderRadius: '10px',
      padding: '16px 20px',
      boxShadow: '0 1px 2px rgba(0, 0, 0, 0.04)',
    },
    statNum: {
      fontSize: '26px',
      fontWeight: '700',
      color: '#1e293b',
      lineHeight: 1.2,
    },
    statLabel: {
      fontSize: '13px',
      color: '#64748b',
      marginTop: '4px',
    },
    filterCard: {
      backgroundColor: '#ffffff',
      border: '1px solid #e2e8f0',
      borderRadius: '10px',
      padding: '18px 20px',
      marginBottom: '20px',
    },
    tableCard: {
      backgroundColor: '#ffffff',
      border: '1px solid #e2e8f0',
      borderRadius: '10px',
      overflow: 'hidden',
      boxShadow: '0 1px 3px rgba(0, 0, 0, 0.05)',
    },
  };

  return (
    <div>
      {/* Header */}
      <div style={styles.header}>
        <div>
          <h1 style={styles.title}>Quản lý tài khoản khách hàng</h1>
          <div style={styles.subtitle}>Danh sách và trạng thái tài khoản khách hàng thành viên</div>
        </div>
      </div>

      {/* Alerts */}
      {notice && (
        <Alert variant="success" dismissible onClose={() => setNotice('')} className="mb-4">
          {notice}
        </Alert>
      )}
      {error && (
        <Alert variant="danger" dismissible onClose={() => setError('')} className="mb-4">
          {error}
        </Alert>
      )}

      {/* Stats */}
      <div style={styles.statGrid}>
        <div style={styles.statCard}>
          <div style={styles.statNum}>{stats.total}</div>
          <div style={styles.statLabel}>Tổng số khách hàng</div>
        </div>
        <div style={styles.statCard}>
          <div style={{ ...styles.statNum, color: '#10b981' }}>{stats.active}</div>
          <div style={styles.statLabel}>Khách hàng đang hoạt động</div>
        </div>
        <div style={styles.statCard}>
          <div style={{ ...styles.statNum, color: stats.locked > 0 ? '#ef4444' : '#64748b' }}>
            {stats.locked}
          </div>
          <div style={styles.statLabel}>Tài khoản bị khóa</div>
        </div>
      </div>

      {/* Filters */}
      <div style={styles.filterCard}>
        <div className="d-flex flex-wrap gap-3 align-items-center justify-content-between">
          <div style={{ minWidth: '280px', flex: '1 1 320px' }}>
            <Form.Control
              type="text"
              placeholder="🔍 Tìm theo họ tên, email khách hàng..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setCurrentPage(1);
              }}
            />
          </div>
          <div className="d-flex gap-3">
            <Form.Select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setCurrentPage(1);
              }}
              style={{ width: '180px' }}
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="active">Đang hoạt động</option>
              <option value="locked">Đã bị khóa</option>
            </Form.Select>
          </div>
        </div>
      </div>

      {/* Table */}
      <div style={styles.tableCard}>
        {loading ? (
          <div className="text-center py-5">
            <Spinner animation="border" variant="primary" />
            <div className="text-secondary mt-2">Đang tải danh sách khách hàng...</div>
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-5 text-secondary">
            Không tìm thấy khách hàng nào phù hợp với điều kiện tìm kiếm.
          </div>
        ) : (
          <Table responsive hover className="mb-0 align-middle">
            <thead style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
              <tr style={{ color: '#475569', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                <th style={{ padding: '14px 18px' }}># ID</th>
                <th style={{ padding: '14px 18px' }}>Họ và tên</th>
                <th style={{ padding: '14px 18px' }}>Email</th>
                <th style={{ padding: '14px 18px' }}>Trạng thái</th>
                <th style={{ padding: '14px 18px' }}>Ngày đăng ký</th>
                <th style={{ padding: '14px 18px', textAlign: 'right' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {paginated.map((u) => {
                const isLocked = u.status === 'locked' || Boolean(u.locked);

                return (
                  <tr key={u.id}>
                    <td style={{ padding: '14px 18px', color: '#64748b', fontWeight: '500' }}>#{u.id}</td>
                    <td style={{ padding: '14px 18px' }}>
                      <div className="fw-semibold text-dark">{u.fullName || 'Khách hàng'}</div>
                    </td>
                    <td style={{ padding: '14px 18px', color: '#334155' }}>{u.email}</td>
                    <td style={{ padding: '14px 18px' }}>
                      <Badge bg={isLocked ? 'danger' : 'success'}>
                        {isLocked ? 'Đã khóa' : 'Hoạt động'}
                      </Badge>
                    </td>
                    <td style={{ padding: '14px 18px', color: '#64748b', fontSize: '13px' }}>
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString('vi-VN') : '—'}
                    </td>
                    <td style={{ padding: '14px 18px', textAlign: 'right' }}>
                      <Button
                        size="sm"
                        variant={isLocked ? 'outline-success' : 'outline-danger'}
                        onClick={() => handleToggleStatus(u)}
                      >
                        {isLocked ? 'Mở khóa' : 'Khóa tài khoản'}
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        )}
      </div>

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="d-flex justify-content-between align-items-center mt-3">
          <div className="text-secondary small">
            Hiển thị {startIndex + 1} - {Math.min(startIndex + pageSize, filtered.length)} trên tổng số {filtered.length} khách hàng
          </div>
          <div className="d-flex gap-1">
            <Button
              size="sm"
              variant="outline-secondary"
              disabled={validPage <= 1}
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
            >
              Trang trước
            </Button>
            <span className="px-3 py-1 text-secondary small align-self-center">
              Trang {validPage} / {totalPages}
            </span>
            <Button
              size="sm"
              variant="outline-secondary"
              disabled={validPage >= totalPages}
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
            >
              Trang sau
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

