// The Java API uses camelCase; the existing inventory view uses SQL-style keys.
// Map only the inventory snapshot, never mock/catalog store data.
export function mapInventorySnapshot(snapshot) {
  const toSnake = (value) => {
    if (Array.isArray(value)) return value.map(toSnake);
    if (value && typeof value === 'object') {
      return Object.fromEntries(
        Object.entries(value).map(([key, item]) => [
          key.replace(/[A-Z]/g, (letter) => '_' + letter.toLowerCase()),
          toSnake(item),
        ]),
      );
    }
    return value;
  };
  return toSnake(snapshot);
}
