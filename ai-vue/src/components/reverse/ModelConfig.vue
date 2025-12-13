<template>
  <div class="card" style="margin-bottom: 20px;">
    <div class="card-title">模型配置信息 (对应 dim_ai_model 表)</div>
    <div class="grid" style="grid-template-columns: repeat(2, 1fr); gap: 16px;">
      <!-- 模型昵称 -->
      <div class="input-group">
        <label>模型昵称 (model_name)</label>
        <input
          v-model="modelName"
          type="text"
          placeholder="例如: doubao-seedance-1-0-pro-250528"
          @input="handleChange"
        />
      </div>

      <!-- 原始模型名称 -->
      <div class="input-group">
        <label>原始模型名称 (origin_name)</label>
        <input
          v-model="originName"
          type="text"
          placeholder="例如: 豆包舞蹈模型"
          @input="handleChange"
        />
      </div>

      <!-- 请求地址 -->
      <div class="input-group">
        <label>请求地址 (base_url)</label>
        <input
          v-model="baseUrl"
          type="text"
          placeholder="例如: https://ark.cn-beijing.volces.com/api/v3"
          @input="handleChange"
        />
      </div>

      <!-- 端点 -->
      <div class="input-group">
        <label>端点 (point)</label>
        <input
          v-model="point"
          type="text"
          placeholder="例如: /tasks"
          @input="handleChange"
        />
      </div>

      <!-- Authorization Token -->
      <div class="input-group" style="grid-column: 1 / -1;">
        <label>Authorization Token (authorization)</label>
        <input
          v-model="authorization"
          type="text"
          placeholder="例如: Bearer $ARK_API_KEY"
          @input="handleChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelName: {
    type: String,
    default: ''
  },
  originName: {
    type: String,
    default: ''
  },
  baseUrl: {
    type: String,
    default: ''
  },
  point: {
    type: String,
    default: ''
  },
  authorization: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelName', 'update:originName', 'update:baseUrl', 'update:point', 'update:authorization'])

const modelName = ref(props.modelName || '')
const originName = ref(props.originName || '')
const baseUrl = ref(props.baseUrl || '')
const point = ref(props.point || '')
const authorization = ref(props.authorization || '')

// 监听外部变化
watch(() => props.modelName, (newVal) => {
  modelName.value = newVal || ''
})
watch(() => props.originName, (newVal) => {
  originName.value = newVal || ''
})
watch(() => props.baseUrl, (newVal) => {
  baseUrl.value = newVal || ''
})
watch(() => props.point, (newVal) => {
  point.value = newVal || ''
})
watch(() => props.authorization, (newVal) => {
  authorization.value = newVal || ''
})

const handleChange = () => {
  emit('update:modelName', modelName.value)
  emit('update:originName', originName.value)
  emit('update:baseUrl', baseUrl.value)
  emit('update:point', point.value)
  emit('update:authorization', authorization.value)
}
</script>

