import { useState, useEffect, useCallback } from 'react';
import { Table, Badge, Button, Form, Alert, Spinner } from 'react-bootstrap';
import { customerApi } from '@/modules/admin/api/admin.api';
import { ManagerSearch, ManagerPagination } from '@/shared/components/ManagerControls';

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

  // Reset to first page when search or filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [search, statusFilter]);

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

  return (
    <section className="manager-page">
      <div className="eyebrow">HỆ THỐNG QUẢN TRỊ</div>

      {/* Header */}
      <div className="manager-title">
        <div>
          <h1>Quản lý tài khoản khách hàng</h1>
          <p>Danh sách và trạng thái tài khoản khách hàng thành viên.</p>
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
      <div className="manager-stats" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
        <div className="manager-panel">
          <span className="text-secondary">Tổng số khách hàng</span>
          <strong>{stats.total}</strong>
        </div>
        <div className="manager-panel">
          <span className="text-secondary">Khách hàng đang hoạt động</span>
          <strong className="text-success">{stats.active}</strong>
        </div>
        <div className="manager-panel">
          <span className="text-secondary">Tài khoản bị khóa</span>
          <strong className={stats.locked > 0 ? 'text-danger' : 'text-secondary'}>
            {stats.locked}
          </strong>
        </div>
      </div>

      {/* Filters & Table Panel */}
      <div className="manager-panel">
        <div className="manager-filters">
          <ManagerSearch
            placeholder="Tìm theo họ tên, email khách hàng…"
            label="Tìm khách hàng"
            value={search}
            onChange={(val) => {
              setSearch(val);
              setCurrentPage(1);
            }}
          />
          <Form.Select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setCurrentPage(1);
            }}
            style={{ maxWidth: '200px' }}
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="active">Đang hoạt động</option>
            <option value="locked">Đã bị khóa</option>
          </Form.Select>
        </div>

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
          <Table responsive hover className="manager-table mb-0 align-middle">
            <thead>
              <tr>
                <th style={{ width: '60px' }}>STT</th>
                <th>Họ và tên</th>
                <th>Email</th>
                <th>Trạng thái</th>
                <th>Ngày đăng ký</th>
                <th style={{ textAlign: 'right' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {paginated.map((u, index) => {
                const isLocked = u.status === 'locked' || Boolean(u.locked);

                return (
                  <tr key={u.id}>
                    <td className="text-secondary fw-semibold">
                      {startIndex + index + 1}
                    </td>
                    <td>
                      <div className="fw-semibold text-dark">{u.fullName || 'Khách hàng'}</div>
                    </td>
                    <td>{u.email}</td>
                    <td>
                      <Badge bg={isLocked ? 'danger' : 'success'}>
                        {isLocked ? 'Đã khóa' : 'Hoạt động'}
                      </Badge>
                    </td>
                    <td className="text-secondary">
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString('vi-VN') : '—'}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <Button
                        size="sm"
                        variant={isLocked ? 'outline-success' : 'outline-danger'}
                        onClick={() => handleToggleStatus(u)}
                      >
                        {isLocked ? 'Mở khóa' : 'Khóa'}
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        )}

        {/* Pagination Controls */}
        <ManagerPagination
          total={filtered.length}
          page={validPage}
          pageSize={pageSize}
          onChange={setCurrentPage}
        />
      </div>
    </section>
  );
}

