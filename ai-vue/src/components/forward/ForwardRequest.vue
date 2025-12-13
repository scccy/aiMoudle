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
import { generateRequest as generateRequestApi } from '../../api/forwardRequest.js'

const selectedModel = ref('')
const payload = ref({})
const requestResult = ref(null)
const loading = ref(false)
const error = ref('')

// 处理模型变化
const handleModelChange = (modelName) => {
  // 可以在这里加载模型配置，预填充一些默认参数
  console.log('模型已选择:', modelName)
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
