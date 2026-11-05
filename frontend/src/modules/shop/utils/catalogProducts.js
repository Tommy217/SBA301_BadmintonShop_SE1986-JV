// Original demo seed records remain available for historical order references.
const demoSlugs = new Set([
  'yonex-astrox-88d-play-1', 'victor-thruster-k-220h-2', 'li-ning-windstorm-72-3',
  'yonex-power-cushion-39-4', 'victor-a170-ii-5', 'ao-cau-long-training-essential-6',
  'demo-product-racket', 'demo-product-shoes', 'demo-product-apparel',
]);
export function isDemoProduct(product) { return demoSlugs.has(product.slug); }
