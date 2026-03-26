import { defineStore } from 'pinia'
import { ref } from 'vue'
import { countryAll, vendorList, channelList, sidList, customerList } from '../api'

/**
 * Global reference data cache.
 * Dropdown options (countries, vendors, channels, etc.) are loaded once
 * and shared across all pages to avoid redundant API calls on every route change.
 *
 * Call refresh('countries') to force reload after CRUD operations.
 */
export const useRefData = defineStore('refData', () => {
  const countries = ref([])
  const vendors = ref([])
  const channels = ref([])
  const sids = ref([])
  const customers = ref([])

  const _loaded = { countries: false, vendors: false, channels: false, sids: false, customers: false }
  const _loading = {}

  async function loadCountries(force = false) {
    if (_loaded.countries && !force) return countries.value
    if (_loading.countries) return _loading.countries
    _loading.countries = countryAll().then(r => {
      countries.value = r.data || []
      _loaded.countries = true
      _loading.countries = null
      return countries.value
    }).catch(() => { _loading.countries = null; return countries.value })
    return _loading.countries
  }

  async function loadVendors(force = false) {
    if (_loaded.vendors && !force) return vendors.value
    if (_loading.vendors) return _loading.vendors
    _loading.vendors = vendorList({ page: 1, size: 500 }).then(r => {
      vendors.value = r.data?.list || []
      _loaded.vendors = true
      _loading.vendors = null
      return vendors.value
    }).catch(() => { _loading.vendors = null; return vendors.value })
    return _loading.vendors
  }

  async function loadChannels(force = false) {
    if (_loaded.channels && !force) return channels.value
    if (_loading.channels) return _loading.channels
    _loading.channels = channelList({ page: 1, size: 500 }).then(r => {
      channels.value = r.data?.list || []
      _loaded.channels = true
      _loading.channels = null
      return channels.value
    }).catch(() => { _loading.channels = null; return channels.value })
    return _loading.channels
  }

  async function loadSids(force = false) {
    if (_loaded.sids && !force) return sids.value
    if (_loading.sids) return _loading.sids
    _loading.sids = sidList({ page: 1, size: 500 }).then(r => {
      sids.value = r.data?.list || []
      _loaded.sids = true
      _loading.sids = null
      return sids.value
    }).catch(() => { _loading.sids = null; return sids.value })
    return _loading.sids
  }

  async function loadCustomers(force = false) {
    if (_loaded.customers && !force) return customers.value
    if (_loading.customers) return _loading.customers
    _loading.customers = customerList({ page: 1, size: 500 }).then(r => {
      customers.value = r.data?.list || []
      _loaded.customers = true
      _loading.customers = null
      return customers.value
    }).catch(() => { _loading.customers = null; return customers.value })
    return _loading.customers
  }

  function refresh(key) {
    if (key) { _loaded[key] = false }
    else { Object.keys(_loaded).forEach(k => _loaded[k] = false) }
  }

  return { countries, vendors, channels, sids, customers, loadCountries, loadVendors, loadChannels, loadSids, loadCustomers, refresh }
})
