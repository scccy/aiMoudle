import axios from 'axios'

const API_BASE_URL = '/api/dictionary'

/**
 * 查询字典映射关系（分页）
 * @param {Object} params - 查询参数
 * @param {number} params.pageSize - 每页大小
 * @param {number} params.pageNumber - 页码（从1开始）
 * @param {string} params.modelName - 模型名称
 * @param {string} params.key - key
 * @param {string} params.postParam - post_param
 * @returns {Promise}
 */
export const searchDictionary = async (params = {}) => {
  try {
    // 分离查询参数和请求体
    const { pageSize, pageNumber, ...requestBody } = params
    const response = await axios.post(`${API_BASE_URL}/search`, requestBody, {
      params: {
        pageSize,
        pageNumber
      }
    })
    return response.data
  } catch (error) {
    console.error('Search dictionary error:', error)
    throw error
  }
}

/**
 * 获取 Item 详情
 * @param {string|number} itemId - item id (作为字符串处理，避免精度丢失)
 * @returns {Promise}
 */
export const getItemDetail = async (itemId) => {
  try {
    // 确保 itemId 作为字符串传递，避免精度丢失
    const itemIdStr = String(itemId)
    const response = await axios.get(`${API_BASE_URL}/item/${itemIdStr}`)
    return response.data
  } catch (error) {
    console.error('Get item detail error:', error)
    throw error
  }
}

/**
 * 更新 Item 详情
 * @param {string|number} itemId - item id (作为字符串处理，避免精度丢失)
 * @param {Object} data - 更新数据
 * @returns {Promise}
 */
export const updateItemDetail = async (itemId, data) => {
  try {
    // 确保 itemId 作为字符串传递，避免精度丢失
    const itemIdStr = String(itemId)
    const response = await axios.put(`${API_BASE_URL}/item/${itemIdStr}`, data)
    return response.data
  } catch (error) {
    console.error('Update item detail error:', error)
    throw error
  }
}

/**
 * 创建字典映射
 * @param {Object} data - 映射数据
 * @param {string} data.key - key
 * @param {string} data.postParam - post_param
 * @param {string} data.description - 中文说明
 * @param {string|number} data.itemId - item id
 * @returns {Promise}
 */
export const createDictionary = async (data) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/create`, data)
    return response.data
  } catch (error) {
    console.error('Create dictionary error:', error)
    throw error
  }
}

/**
 * 更新字典映射
 * @param {Object} data - 映射数据
 * @param {string} data.key - key
 * @param {string} data.postParam - post_param
 * @param {string} data.description - 中文说明
 * @param {string|number} data.itemId - item id
 * @returns {Promise}
 */
export const updateDictionary = async (data) => {
  try {
    const response = await axios.put(`${API_BASE_URL}/update`, data)
    return response.data
  } catch (error) {
    console.error('Update dictionary error:', error)
    throw error
  }
}

/**
 * 删除字典映射
 * @param {string} key - key
 * @param {string} postParam - post_param
 * @returns {Promise}
 */
export const deleteDictionary = async (key, postParam) => {
  try {
    const response = await axios.delete(`${API_BASE_URL}/delete`, {
      params: { key, postParam }
    })
    return response.data
  } catch (error) {
    console.error('Delete dictionary error:', error)
    throw error
  }
}

/**
 * 获取字典查询选项（用于下拉选择框）
 * @returns {Promise}
 */
export const getDictionaryOptions = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/options`)
    return response.data
  } catch (error) {
    console.error('Get dictionary options error:', error)
    throw error
  }
}

