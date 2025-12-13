<template>
  <div class="card">
    <div class="card-title">Request Body (JSON)</div>
    <div class="input-group">
      <textarea
        v-model="bodyText"
        placeholder='{"model": "example", "content": [{"type": "text", "text": "hello"}]}'
        @input="handleBodyChange"
        class="json-textarea"
      ></textarea>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  requestBody: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:requestBody', 'autoFillParamMapping', 'requestBasicMapping'])

const bodyText = ref(JSON.stringify(props.requestBody, null, 2))

// 监听外部变化
watch(() => props.requestBody, (newVal) => {
  try {
    const newText = JSON.stringify(newVal, null, 2)
    if (newText !== bodyText.value) {
      bodyText.value = newText
      // 注释掉自动生成逻辑，改为由后端自动解析
      // 如果用户需要，可以手动点击"自动填充"按钮
      // if (newVal && typeof newVal === 'object' && Object.keys(newVal).length > 0) {
      //   const mappings = parseToMappings(newVal)
      //   emit('autoFillParamMapping', mappings)
      // }
    }
  } catch (e) {
    // 忽略解析错误
  }
}, { deep: true })

const handleBodyChange = () => {
  try {
    const parsed = JSON.parse(bodyText.value)
    emit('update:requestBody', parsed)
    
    // 如果解析成功，请求生成基础映射
    if (parsed && typeof parsed === 'object' && Object.keys(parsed).length > 0) {
      emit('requestBasicMapping')
    }
  } catch (e) {
    // 解析错误时不清空，让用户继续编辑
  }
}
</script>
