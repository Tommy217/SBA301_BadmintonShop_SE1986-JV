import { Navigate, useParams } from 'react-router';
import CatalogSection from '@/modules/management/components/CatalogSection';
import ProductManagementPage from '@/modules/management/components/ProductCatalog';
import VoucherManagementPage from '@/modules/management/components/VoucherCatalog';
import { legacyCatalogs } from '@/modules/management/utils/catalogNavigation';
import CatalogProvider from '../components/CatalogProvider';
export default function CatalogPage() {
  return <CatalogProvider><CatalogContent /></CatalogProvider>;
}
export function CatalogContent() {
  const { catalog } = useParams();
  const legacy = legacyCatalogs[catalog];
  if (legacy)
    return <Navigate replace to={'/management/catalog/' + legacy.parent + '?tab=' + legacy.tab} />;
  if (catalog === 'products') return <ProductManagementPage />;
  if (catalog === 'vouchers') return <VoucherManagementPage />;
  return <CatalogSection key={catalog} catalog={catalog} />;
}
