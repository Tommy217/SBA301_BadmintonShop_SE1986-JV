export function productImage(data, product, variantId) {
  const sameId = (a, b) => String(a) === String(b);
  const images = (data.product_images || [])
    .filter((image) => sameId(image.product_id, product.id) && image.url?.trim() && !/^\/products\/(racket|shoes|apparel)\.svg$/.test(image.url))
    .sort((a, b) => Number(a.sort_order ?? 0) - Number(b.sort_order ?? 0) || Number(a.id) - Number(b.id));
  const image = (variantId != null && images.find((i) => sameId(i.variant_id, variantId))) ||
    images.find((i) => i.variant_id == null) || images[0];
  return image?.url || '/products/unavailable.svg';
}

export function productImageFallback(event) {
  const fallback = '/products/unavailable.svg';
  if (event.currentTarget.getAttribute('src') !== fallback) event.currentTarget.src = fallback;
}
