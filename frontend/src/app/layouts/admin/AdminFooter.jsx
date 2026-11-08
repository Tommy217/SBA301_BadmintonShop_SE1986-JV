const styles = {
  footer: {
    marginTop: 'auto',
    padding: '16px 24px',
    backgroundColor: '#ffffff',
    borderTop: '1px solid #e2e8f0',
    textAlign: 'center',
    fontSize: '13px',
    color: '#64748b',
  },
};

export default function AdminFooter() {
  return (
    <footer style={styles.footer}>
      <span>&copy; {new Date().getFullYear()} Badminton Shop &middot; Quản Trị Hệ Thống</span>
    </footer>
  );
}
