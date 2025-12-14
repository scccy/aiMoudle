<template>
  <div class="forward-request-page">
    <div class="container">
      <h1 style="text-align: center; margin-bottom: 30px; color: #1a1a1a;">
        正向请求（模拟 POST 请求）
      </h1>
      
      <!-- 模型选择器 -->
      <ModelSelector 
        v-model:modelName="selectedModel"
        @model-change="handleModelChange"
      />

      <!-- 参数表单 -->
      <ParameterForm 
        v-if="selectedModel"
        v-model="payload"
        style="margin-top: 20px;"
      />

      <!-- 生成请求按钮 -->
      <div v-if="selectedModel" class="card" style="text-align: center; margin-top: 20px;">
        <button 
          class="btn btn-primary" 
          @click="generateRequest" 
          :disabled="loading"
          style="width: 200px;"
        >
          {{ loading ? '生成中...' : '生成请求' }}
        </button>
      </div>

      <!-- 错误提示 -->
      <div v-if="error" class="error" style="margin-top: 20px;">
        {{ error }}
      </div>

      <!-- 请求结果 -->
      <RequestResult 
        v-if="requestResult"
        :result="requestResult"
        style="margin-top: 20px;"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ModelSelector from './ModelSelector.vue'
import ParameterForm from './ParameterForm.vue'
import RequestResult from './RequestResult.vue'
import { generateRequest as generateRequestApi, getModelBaseParams } from '../../api/forwardRequest.js'

const selectedModel = ref('')
const payload = ref({})
const requestResult = ref(null)
const loading = ref(false)
const error = ref('')

// 转换后端返回的特殊格式为标准格式
// xxx[] -> [] (数组)
// map{1} 或 map{*} -> {} (对象)
const transformBaseParams = (data) => {
  if (!data || typeof data !== 'object') {
    return data
  }
  
  const result = {}
  
  for (const [key, value] of Object.entries(data)) {
    // 检查是否是数组格式 xxx[]
    if (key.endsWith('[]')) {
      const arrayKey = key.slice(0, -2) // 移除 []
      
      // 如果值是对象，需要处理 map{1} 或 map{*}
      if (value && typeof value === 'object' && !Array.isArray(value)) {
        const arrayValue = []
        
        // 遍历对象的所有键
        for (const [mapKey, mapValue] of Object.entries(value)) {
          // 检查是否是 map{1} 或 map{*} 格式
          if (mapKey.startsWith('map{') && mapKey.endsWith('}')) {
            // 提取 map 中的值（递归转换）
            const transformedValue = transformBaseParams(mapValue)
            arrayValue.push(transformedValue)
          } else {
            // 如果不是 map{} 格式，直接添加（递归转换）
            const transformedValue = transformBaseParams(mapValue)
            arrayValue.push(transformedValue)
          }
        }
        
        result[arrayKey] = arrayValue
      } else if (Array.isArray(value)) {
        // 如果已经是数组，递归转换每个元素
        result[arrayKey] = value.map(item => transformBaseParams(item))
      } else {
        // 其他情况，直接转换
        result[arrayKey] = transformBaseParams(value)
      }
    } else {
      // 普通键，递归转换值
      result[key] = transformBaseParams(value)
    }
  }
  
  return result
}

// 处理模型变化
const handleModelChange = async (modelName) => {
  console.log('模型已选择:', modelName)
  
  if (!modelName) {
    payload.value = {}
    return
  }
  
  // 从后端获取已生成的基础请求参数
  try {
    loading.value = true
    error.value = ''
    const response = await getModelBaseParams(modelName)
    
    if (response.code === 200 && response.data) {
      // 转换特殊格式为标准格式
      const transformedData = transformBaseParams(response.data)
      payload.value = transformedData || {}
      console.log('原始数据:', response.data)
      console.log('转换后的数据:', payload.value)
    } else {
      // 如果没有配置，清空参数
      payload.value = {}
    }
  } catch (err) {
    console.error('加载模型基础参数失败:', err)
    error.value = '加载模型基础参数失败: ' + (err.message || '未知错误')
    payload.value = {}
  } finally {
    loading.value = false
  }
}

// 生成请求
const generateRequest = async () => {
  if (!selectedModel.value) {
    error.value = '请先选择模型'
    return
  }
  
  if (!payload.value || Object.keys(payload.value).length === 0) {
    error.value = '请至少添加一个参数'
    return
  }
  
  error.value = ''
  loading.value = true
  requestResult.value = null
  
  try {
    const response = await generateRequestApi(selectedModel.value, payload.value)
    
    if (response.code === 200) {
      requestResult.value = response.data
    } else {
      error.value = response.message || '生成请求失败'
    }
  } catch (err) {
    error.value = err.message || '请求失败'
    console.error('Generate request error:', err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forward-request-page {
  padding: 20px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.error {
  background: #fee;
  color: #c33;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #fcc;
}
</style>
