import { mergeShopSnapshot } from './shopSnapshot.js';

// A catalog refresh failure must not make a successfully saved checkout look failed.
export async function commitShopAction(save, refreshCatalog) {
  const snapshot = await save();
  const catalog = await refreshCatalog().catch(() => null);
  return { ...mergeShopSnapshot(snapshot, catalog || {}), catalogRefreshFailed: catalog === null };
}
