import { useState, useEffect, useCallback } from 'react';
import { Table, Badge, Button, Form, Alert, Spinner } from 'react-bootstrap';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { employeeApi } from '@/modules/admin/api/admin.api';
import EmployeeModal from '@/modules/admin/components/EmployeeModal';
import { ManagerSearch, ManagerPagination } from '@/shared/components/ManagerControls';
import { ROLES } from '@/shared/constants/roles';

export default function EmployeesPage() {
  const { user: currentAdmin } = useAuth();

  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  // Filters
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  // Modal
  const [showModal, setShowModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await employeeApi.getEmployees({ size: 1000 });
      const internalList = Array.isArray(response) ? response : response?.content || [];
      setEmployees(internalList);
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách nhân viên.');
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
  }, [search, roleFilter, statusFilter]);

  // Statistics
  const stats = {
    total: employees.length,
    admins: employees.filter((u) => u.role === ROLES.ADMIN).length,
    managers: employees.filter((u) => u.role === ROLES.MANAGER).length,
    staff: employees.filter((u) => u.role === ROLES.STAFF).length,
    locked: employees.filter((u) => u.status === 'locked' || u.locked).length,
  };

  // Filtered employees
  const filtered = employees.filter((u) => {
    const matchSearch =
      search.trim() === '' ||
      String(u.fullName || '').toLowerCase().includes(search.toLowerCase()) ||
      String(u.email || '').toLowerCase().includes(search.toLowerCase());

    const matchRole = roleFilter === 'ALL' || u.role === roleFilter;

    const isLocked = u.status === 'locked' || Boolean(u.locked);
    const matchStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'active' && !isLocked) ||
      (statusFilter === 'locked' && isLocked);

    return matchSearch && matchRole && matchStatus;
  });

  // Pagination
  const totalPages = Math.ceil(filtered.length / pageSize) || 1;
  const validPage = Math.min(Math.max(1, currentPage), totalPages);
  const startIndex = (validPage - 1) * pageSize;
  const paginated = filtered.slice(startIndex, startIndex + pageSize);

  const handleCreate = () => {
    setSelectedUser(null);
    setShowModal(true);
  };

  const handleEdit = (u) => {
    setSelectedUser(u);
    setShowModal(true);
  };

  const handleSave = async (payload) => {
    if (selectedUser) {
      await employeeApi.updateEmployee(selectedUser.id, payload);
      setNotice(`Đã cập nhật thành công nhân viên: ${selectedUser.email}`);
    } else {
      await employeeApi.createEmployee(payload);
      setNotice(`Đã tạo mới thành công tài khoản nhân viên: ${payload.email}`);
    }
    await loadData();
  };

  const handleToggleStatus = async (u) => {
    const isCurrentlyLocked = u.status === 'locked' || Boolean(u.locked);
    const newStatus = isCurrentlyLocked ? 'active' : 'locked';

    if (currentAdmin && String(u.id) === String(currentAdmin.id)) {
      setError('Không thể tự khóa tài khoản của chính mình.');
      return;
    }

    try {
      await employeeApi.updateEmployeeStatus(u.id, newStatus);
      setNotice(`Đã ${newStatus === 'active' ? 'mở khóa' : 'khóa'} tài khoản ${u.email}.`);
      await loadData();
    } catch (err) {
      setError(err.message || 'Không thể cập nhật trạng thái nhân viên.');
    }
  };

  const roleBadge = (role) => {
    const config = {
      [ROLES.ADMIN]: { bg: 'danger', label: 'Quản trị viên (ADMIN)' },
      [ROLES.MANAGER]: { bg: 'primary', label: 'Quản lý (MANAGER)' },
      [ROLES.STAFF]: { bg: 'info', label: 'Nhân viên (STAFF)' },
    }[role] || { bg: 'secondary', label: role };

    return <Badge bg={config.bg}>{config.label}</Badge>;
  };

  return (
    <section className="manager-page">
      <div className="eyebrow">HỆ THỐNG QUẢN TRỊ</div>

      {/* Header */}
      <div className="manager-title">
        <div>
          <h1>Quản lý nhân viên nội bộ</h1>
          <p>Quản lý thông tin tài khoản Quản trị viên, Quản lý và Nhân viên.</p>
        </div>
        <Button variant="primary" onClick={handleCreate}>
          + Thêm nhân viên
        </Button>
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
      <div className="manager-stats" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))' }}>
        <div className="manager-panel">
          <span className="text-secondary">Tổng số nhân sự</span>
          <strong>{stats.total}</strong>
        </div>
        <div className="manager-panel">
          <span className="text-secondary">Quản trị viên</span>
          <strong className="text-danger">{stats.admins}</strong>
        </div>
        <div className="manager-panel">
          <span className="text-secondary">Quản lý</span>
          <strong style={{ color: '#2386c8' }}>{stats.managers}</strong>
        </div>
        <div className="manager-panel">
          <span className="text-secondary">Nhân viên</span>
          <strong style={{ color: '#0ea5e9' }}>{stats.staff}</strong>
        </div>
        <div className="manager-panel">
          <span className="text-secondary">Tài khoản bị khóa</span>
          <strong className={stats.locked > 0 ? 'text-danger' : 'text-success'}>
            {stats.locked}
          </strong>
        </div>
      </div>

      {/* Filters & Table Panel */}
      <div className="manager-panel">
        <div className="manager-filters">
          <ManagerSearch
            placeholder="Tìm theo họ tên, email…"
            label="Tìm nhân viên"
            value={search}
            onChange={(val) => {
              setSearch(val);
              setCurrentPage(1);
            }}
          />
          <Form.Select
            value={roleFilter}
            onChange={(e) => {
              setRoleFilter(e.target.value);
              setCurrentPage(1);
            }}
            style={{ maxWidth: '200px' }}
          >
            <option value="ALL">Tất cả vai trò</option>
            <option value={ROLES.ADMIN}>Quản trị viên</option>
            <option value={ROLES.MANAGER}>Quản lý</option>
            <option value={ROLES.STAFF}>Nhân viên</option>
          </Form.Select>

          <Form.Select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setCurrentPage(1);
            }}
            style={{ maxWidth: '190px' }}
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="active">Đang hoạt động</option>
            <option value="locked">Đã bị khóa</option>
          </Form.Select>
        </div>

        {loading ? (
          <div className="text-center py-5">
            <Spinner animation="border" variant="primary" />
            <div className="text-secondary mt-2">Đang tải danh sách nhân viên...</div>
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-5 text-secondary">
            Không tìm thấy nhân viên nào phù hợp với điều kiện tìm kiếm.
          </div>
        ) : (
          <Table responsive hover className="manager-table mb-0 align-middle">
            <thead>
              <tr>
                <th style={{ width: '60px' }}>STT</th>
                <th>Nhân viên</th>
                <th>Email</th>
                <th>Vai trò</th>
                <th>Trạng thái</th>
                <th>Ngày tạo</th>
                <th style={{ textAlign: 'right' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {paginated.map((u, index) => {
                const isSelf = currentAdmin && String(u.id) === String(currentAdmin.id);
                const isLocked = u.status === 'locked' || Boolean(u.locked);

                return (
                  <tr key={u.id}>
                    <td className="text-secondary fw-semibold">
                      {startIndex + index + 1}
                    </td>
                    <td>
                      <div className="fw-semibold text-dark">
                        {u.fullName || 'Chưa đặt tên'}
                        {isSelf && (
                          <Badge bg="secondary" className="ms-2" style={{ fontSize: '11px' }}>
                            Bạn
                          </Badge>
                        )}
                      </div>
                    </td>
                    <td>{u.email}</td>
                    <td>{roleBadge(u.role)}</td>
                    <td>
                      <Badge bg={isLocked ? 'danger' : 'success'}>
                        {isLocked ? 'Đã khóa' : 'Hoạt động'}
                      </Badge>
                    </td>
                    <td className="text-secondary">
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString('vi-VN') : '—'}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div className="d-flex justify-content-end gap-2">
                        <Button
                          size="sm"
                          variant="outline-primary"
                          onClick={() => handleEdit(u)}
                        >
                          Sửa
                        </Button>
                        <Button
                          size="sm"
                          variant={isLocked ? 'outline-success' : 'outline-danger'}
                          disabled={isSelf}
                          title={isSelf ? 'Không thể tự khóa tài khoản của chính mình' : undefined}
                          onClick={() => handleToggleStatus(u)}
                        >
                          {isLocked ? 'Mở khóa' : 'Khóa'}
                        </Button>
                      </div>
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

      {/* Modal */}
      <EmployeeModal
        show={showModal}
        onHide={() => setShowModal(false)}
        onSave={handleSave}
        user={selectedUser}
        currentAdminId={currentAdmin?.id}
      />
    </section>
  );
}

