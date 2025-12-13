<template>
  <div id="app">
    <!-- 左侧侧边栏 -->
    <Sidebar :current-page="currentPage" @switch-page="handlePageSwitch" />
    
    <!-- 反向解析页面 -->
    <div v-if="currentPage === 'reverse'" class="main-content" :style="{ marginLeft: '200px' }">
      <div class="container">
      <h1 style="text-align: center; margin-bottom: 30px; color: #1a1a1a;">
        AI Model Reverse Parse Tool
      </h1>

      <!-- 顶部：模型配置信息和 CURL 输入 -->
      <div class="grid" style="grid-template-columns: 1fr 1fr; gap: 20px;">
        <!-- 左侧：CURL 命令输入 -->
        <CurlInput @parseResult="handleCurlParse" />
        
        <!-- 右侧：模型配置信息 -->
        <ModelConfig
          v-model:modelName="formData.modelName"
          v-model:originName="formData.originName"
          v-model:baseUrl="formData.baseUrl"
          v-model:point="formData.point"
          v-model:authorization="formData.authorization"
        />
      </div>

      <!-- 输入区域 -->
      <div class="grid" style="grid-template-columns: 1fr 1fr; gap: 20px;">
        <!-- 左侧：Header 相关 -->
        <div class="left-column">
          <HeaderInput 
            v-model:requestHeaders="formData.requestHeaders"
          />
        </div>

        <!-- 右侧：Body 相关 -->
        <div class="right-column">
          <BodyInput 
            v-model:requestBody="formData.requestBody"
          />
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="card" style="text-align: center;">
        <button class="btn btn-primary" @click="handleGenerate" :disabled="loading">
          {{ loading ? '生成中...' : '生成配置' }}
        </button>
        <button class="btn btn-danger" @click="handleReset" style="margin-left: 10px;">
          重置
        </button>
      </div>

      <!-- 错误提示 -->
      <div v-if="error" class="error">
        {{ error }}
      </div>

      <!-- 下半部分：结果显示 -->
      <div v-if="result" class="card">
        <ResultDisplay 
          :paramItems="result.paramItems"
          :headerItems="result.headerItems"
          :form-data="formData"
          @save="handleSaveResult"
          @confirm-save="handleConfirmSave"
        />
      </div>
      </div>
    </div>
    
    <!-- 正向请求页面 -->
    <div v-if="currentPage === 'forward'" class="main-content" :style="{ marginLeft: '200px' }">
      <ForwardRequest />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import CurlInput from './components/reverse/CurlInput.vue'
import ModelConfig from './components/reverse/ModelConfig.vue'
import HeaderInput from './components/reverse/HeaderInput.vue'
import BodyInput from './components/reverse/BodyInput.vue'
import ResultDisplay from './components/reverse/ResultDisplay.vue'
import { generateConfig, saveConfig } from './api/reverseParse'
import Sidebar from './components/layout/Sidebar.vue'
import ForwardRequest from './components/forward/ForwardRequest.vue'

const formData = reactive({
  modelName: '',
  originName: '',
  baseUrl: '',
  point: '',
  authorization: '',
  requestBody: {},
  requestHeaders: {}
})

const result = ref(null)
const error = ref('')
const loading = ref(false)
const currentPage = ref('reverse')

const handleGenerate = async () => {
  error.value = ''
  result.value = null
  loading.value = true

  try {
    // 基本验证
    if (!formData.requestBody || Object.keys(formData.requestBody).length === 0) {
      throw new Error('请先输入 CURL 命令或填写 Request Body')
    }

    // 直接调用生成配置接口，后端会自动生成完整配置
    const response = await generateConfig({
      requestBody: formData.requestBody,
      requestHeaders: formData.requestHeaders || {}
    })

    if (response.code === 200) {
      result.value = response.data
    } else {
      error.value = response.message || '生成配置失败'
    }
  } catch (err) {
    error.value = err.message || '请求失败，请检查网络连接'
    console.error('Generate error:', err)
  } finally {
    loading.value = false
  }
}

const handleCurlParse = (parseResult) => {
  // 填充模型配置信息
  if (parseResult.baseUrl) {
    formData.baseUrl = parseResult.baseUrl
  }
  if (parseResult.point) {
    formData.point = parseResult.point
  }
  if (parseResult.authorization) {
    formData.authorization = parseResult.authorization
  }
  
  // 填充 Request Headers
  if (parseResult.requestHeaders && Object.keys(parseResult.requestHeaders).length > 0) {
    formData.requestHeaders = parseResult.requestHeaders
  }
  
  // 填充 Request Body
  if (parseResult.requestBody && Object.keys(parseResult.requestBody).length > 0) {
    formData.requestBody = parseResult.requestBody
    // 自动生成配置
    handleGenerate()
  }
}

const handleReset = () => {
  formData.modelName = ''
  formData.originName = ''
  formData.baseUrl = ''
  formData.point = ''
  formData.authorization = ''
  formData.requestBody = {}
  formData.requestHeaders = {}
  result.value = null
  error.value = ''
}

const handlePageSwitch = (page) => {
  currentPage.value = page
}

const handleSaveResult = (savedData) => {
  // 更新结果数据
  if (result.value) {
    result.value.paramItems = savedData.paramItems
    result.value.headerItems = savedData.headerItems
  }
  
  // 可以在这里添加保存到服务器或下载的逻辑
  console.log('保存的配置:', savedData)
}

const handleConfirmSave = async (savedConfigItems) => {
  if (!formData.modelName || !formData.modelName.trim()) {
    alert('请先填写模型昵称 (model_name)')
    return
  }
  
  if (!savedConfigItems || (!savedConfigItems.paramItems || savedConfigItems.paramItems.length === 0) && 
      (!savedConfigItems.headerItems || savedConfigItems.headerItems.length === 0)) {
    alert('请先生成配置')
    return
  }
  
  // 确认保存
  if (!confirm('确认保存配置到数据库？这将覆盖该模型的现有配置。')) {
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    // 使用编辑后的配置项保存
    const saveResponse = await saveConfig({
      modelName: formData.modelName,
      originName: formData.originName,
      baseUrl: formData.baseUrl,
      point: formData.point,
      authorization: formData.authorization,
      paramItems: savedConfigItems.paramItems || [],
      headerItems: savedConfigItems.headerItems || []
    })
    
    if (saveResponse.code === 200) {
      alert('配置已成功保存到数据库！')
    } else {
      error.value = saveResponse.message || '保存失败'
      alert('保存失败: ' + error.value)
    }
  } catch (err) {
    error.value = err.message || '请求失败，请检查网络连接'
    alert('保存失败: ' + error.value)
    console.error('Save error:', err)
  } finally {
    loading.value = false
  }
}
</script>
