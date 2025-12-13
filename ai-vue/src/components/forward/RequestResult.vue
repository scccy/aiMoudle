<template>
  <div v-if="result" class="card">
    <div class="card-title">生成的请求</div>
    
    <div class="result-section">
      <div class="result-item">
        <label>请求 URL:</label>
        <div class="result-content url-content">{{ result.url }}</div>
        <button 
          class="btn btn-sm" 
          style="margin-top: 8px; padding: 4px 8px; font-size: 12px;"
          @click="copyToClipboard(result.url)"
        >
          复制 URL
        </button>
      </div>
      
      <div class="result-item">
        <label>请求 Headers:</label>
        <pre class="result-content">{{ formatJson(result.headers) }}</pre>
        <button 
          class="btn btn-sm" 
          style="margin-top: 8px; padding: 4px 8px; font-size: 12px;"
          @click="copyToClipboard(formatJson(result.headers))"
        >
          复制 Headers
        </button>
      </div>
      
      <div class="result-item">
        <label>请求 Body:</label>
        <pre class="result-content">{{ formatJson(result.body) }}</pre>
        <button 
          class="btn btn-sm" 
          style="margin-top: 8px; padding: 4px 8px; font-size: 12px;"
          @click="copyToClipboard(formatJson(result.body))"
        >
          复制 Body
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  result: {
    type: Object,
    default: null
  }
})

const formatJson = (data) => {
  if (!data) return ''
  if (typeof data === 'string') {
    try {
      const parsed = JSON.parse(data)
      return JSON.stringify(parsed, null, 2)
    } catch (e) {
      return data
    }
  }
  return JSON.stringify(data, null, 2)
}

const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    alert('已复制到剪贴板')
  } catch (err) {
    console.error('复制失败:', err)
    alert('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.result-section {
  margin-top: 20px;
}

.result-item {
  margin-bottom: 24px;
}

.result-item:last-child {
  margin-bottom: 0;
}

.result-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.result-content {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
  overflow-y: auto;
}

.url-content {
  word-break: break-all;
  white-space: normal;
}

.btn-sm {
  background: #f0f0f0;
  border: 1px solid #d9d9d9;
  color: #333;
  cursor: pointer;
}

.btn-sm:hover {
  background: #e0e0e0;
}
</style>

