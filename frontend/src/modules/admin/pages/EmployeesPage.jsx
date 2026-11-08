import { useState, useEffect, useCallback } from 'react';
import { Table, Badge, Button, Form, Alert, Spinner } from 'react-bootstrap';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { employeeApi } from '@/modules/admin/api/admin.api';
import EmployeeModal from '@/modules/admin/components/EmployeeModal';
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
      gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))',
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
          <h1 style={styles.title}>Quản lý nhân viên nội bộ</h1>
          <div style={styles.subtitle}>Quản lý thông tin tài khoản Admin, Quản lý và Nhân viên</div>
        </div>
        <Button
          variant="primary"
          onClick={handleCreate}
          style={{
            backgroundColor: '#2563eb',
            borderColor: '#2563eb',
            padding: '10px 18px',
            fontWeight: 600,
          }}
        >
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
      <div style={styles.statGrid}>
        <div style={styles.statCard}>
          <div style={styles.statNum}>{stats.total}</div>
          <div style={styles.statLabel}>Tổng số nhân sự</div>
        </div>
        <div style={styles.statCard}>
          <div style={{ ...styles.statNum, color: '#dc2626' }}>{stats.admins}</div>
          <div style={styles.statLabel}>Quản trị viên (ADMIN)</div>
        </div>
        <div style={styles.statCard}>
          <div style={{ ...styles.statNum, color: '#2563eb' }}>{stats.managers}</div>
          <div style={styles.statLabel}>Quản lý (MANAGER)</div>
        </div>
        <div style={styles.statCard}>
          <div style={{ ...styles.statNum, color: '#0891b2' }}>{stats.staff}</div>
          <div style={styles.statLabel}>Nhân viên (STAFF)</div>
        </div>
        <div style={styles.statCard}>
          <div style={{ ...styles.statNum, color: stats.locked > 0 ? '#ef4444' : '#10b981' }}>
            {stats.locked}
          </div>
          <div style={styles.statLabel}>Tài khoản bị khóa</div>
        </div>
      </div>

      {/* Filters */}
      <div style={styles.filterCard}>
        <div className="d-flex flex-wrap gap-3 align-items-center justify-content-between">
          <div style={{ minWidth: '260px', flex: '1 1 300px' }}>
            <Form.Control
              type="text"
              placeholder="🔍 Tìm theo họ tên, email..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setCurrentPage(1);
              }}
            />
          </div>
          <div className="d-flex gap-3 flex-wrap">
            <Form.Select
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value);
                setCurrentPage(1);
              }}
              style={{ width: '180px' }}
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
              style={{ width: '170px' }}
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
            <div className="text-secondary mt-2">Đang tải danh sách nhân viên...</div>
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-5 text-secondary">
            Không tìm thấy nhân viên nào phù hợp với điều kiện tìm kiếm.
          </div>
        ) : (
          <Table responsive hover className="mb-0 align-middle">
            <thead style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
              <tr style={{ color: '#475569', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                <th style={{ padding: '14px 18px' }}># ID</th>
                <th style={{ padding: '14px 18px' }}>Nhân viên</th>
                <th style={{ padding: '14px 18px' }}>Email</th>
                <th style={{ padding: '14px 18px' }}>Vai trò</th>
                <th style={{ padding: '14px 18px' }}>Trạng thái</th>
                <th style={{ padding: '14px 18px' }}>Ngày tạo</th>
                <th style={{ padding: '14px 18px', textAlign: 'right' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {paginated.map((u) => {
                const isSelf = currentAdmin && String(u.id) === String(currentAdmin.id);
                const isLocked = u.status === 'locked' || Boolean(u.locked);

                return (
                  <tr key={u.id}>
                    <td style={{ padding: '14px 18px', color: '#64748b', fontWeight: '500' }}>#{u.id}</td>
                    <td style={{ padding: '14px 18px' }}>
                      <div className="fw-semibold text-dark">
                        {u.fullName || 'Chưa đặt tên'}
                        {isSelf && (
                          <Badge bg="secondary" className="ms-2" style={{ fontSize: '11px' }}>
                            Bạn
                          </Badge>
                        )}
                      </div>
                    </td>
                    <td style={{ padding: '14px 18px', color: '#334155' }}>{u.email}</td>
                    <td style={{ padding: '14px 18px' }}>{roleBadge(u.role)}</td>
                    <td style={{ padding: '14px 18px' }}>
                      <Badge bg={isLocked ? 'danger' : 'success'}>
                        {isLocked ? 'Đã khóa' : 'Hoạt động'}
                      </Badge>
                    </td>
                    <td style={{ padding: '14px 18px', color: '#64748b', fontSize: '13px' }}>
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString('vi-VN') : '—'}
                    </td>
                    <td style={{ padding: '14px 18px', textAlign: 'right' }}>
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
      </div>

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="d-flex justify-content-between align-items-center mt-3">
          <div className="text-secondary small">
            Hiển thị {startIndex + 1} - {Math.min(startIndex + pageSize, filtered.length)} trên tổng số {filtered.length} nhân viên
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

      {/* Modal */}
      <EmployeeModal
        show={showModal}
        onHide={() => setShowModal(false)}
        onSave={handleSave}
        user={selectedUser}
        currentAdminId={currentAdmin?.id}
      />
    </div>
  );
}

