import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export const generateConfig = async (data) => {
  try {
    const response = await api.post('/reverse-parse/generate', data)
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


export const saveConfig = async (data) => {
  try {
    const response = await api.post('/reverse-parse/save', data)
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

