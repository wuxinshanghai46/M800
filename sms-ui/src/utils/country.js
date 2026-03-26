import isoCountries from '../data/countries'

/**
 * Returns "CODE - 中文名", e.g. "AD - 安道尔"
 * Falls back to the raw code if not found.
 */
export function getCountryLabel(code) {
  if (!code) return '-'
  const c = isoCountries.find(x => x.code === code)
  return c ? `${c.code} - ${c.name}` : code
}
