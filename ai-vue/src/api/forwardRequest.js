import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 获取模型列表
 */
export const getModelList = async () => {
  try {
    const response = await api.get('/ai-model/list')
    return response.data
  } catch (error) {
    if (error.response) {
      throw new Error(error.response.data.message || '请求失败')
    } else if (error.request) {
      throw new Error('网络错误，请检查服务是否启动')
    } else {
      throw new Error(error.message || '请求失败')
    }
  }
}

/**
 * 生成请求
 */
export const generateRequest = async (modelName, payload) => {
  try {
    const response = await api.post('/ai-model/generate-request', {
      modelName,
      payload
    })
    return response.data
  } catch (error) {
    if (error.response) {
      throw new Error(error.response.data.message || '请求失败')
    } else if (error.request) {
      throw new Error('网络错误，请检查服务是否启动')
    } else {
      throw new Error(error.message || '请求失败')
    }
  }
}

