<template>
  <div class="card">
    <div class="card-title">Request Headers (JSON, 可选)</div>
    <div class="input-group">
      <textarea
        v-model="headersText"
        placeholder='{"Content-Type": "application/json", "Authorization": "Bearer token"}'
        @input="handleHeadersChange"
        class="json-textarea"
      ></textarea>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  requestHeaders: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:requestHeaders', 'autoFillHeaderMapping'])

const headersText = ref(JSON.stringify(props.requestHeaders, null, 2))

// 监听外部变化
watch(() => props.requestHeaders, (newVal) => {
  try {
    const newText = JSON.stringify(newVal, null, 2)
    if (newText !== headersText.value) {
      headersText.value = newText
    }
  } catch (e) {
    // 忽略解析错误
  }
}, { deep: true })

const handleHeadersChange = () => {
  try {
    const parsed = JSON.parse(headersText.value)
    emit('update:requestHeaders', parsed)
    
    // 自动解析并填充 Header Mapping
    if (parsed && typeof parsed === 'object' && Object.keys(parsed).length > 0) {
      const mappings = Object.keys(parsed).map(key => ({
        key: key.toLowerCase().replace(/[^a-z0-9]/g, '') || key, // 生成业务字段名
        postParam: key, // Header 字段名保持不变
        validate: ''
      }))
      emit('autoFillHeaderMapping', mappings)
    }
  } catch (e) {
    // 解析错误时不清空，让用户继续编辑
  }
}
</script>

