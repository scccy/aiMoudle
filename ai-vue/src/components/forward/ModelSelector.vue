<template>
  <div class="card">
    <div class="card-title">选择模型</div>
    <div class="input-group">
      <select 
        v-model="selectedModel" 
        @change="handleModelChange"
        style="width: 100%; padding: 10px; border: 1px solid #d9d9d9; border-radius: 4px;"
      >
        <option value="">请选择模型</option>
        <option v-for="model in modelList" :key="model.modelName" :value="model.modelName">
          {{ model.modelName }} ({{ model.originName }})
        </option>
      </select>
    </div>
    <div v-if="loading" style="margin-top: 10px; color: #666; font-size: 12px;">
      加载中...
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { getModelList } from '../../api/forwardRequest.js'

const props = defineProps({
  modelName: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelName', 'model-change'])

const selectedModel = ref(props.modelName)
const modelList = ref([])
const loading = ref(false)

const handleModelChange = () => {
  emit('update:modelName', selectedModel.value)
  emit('model-change', selectedModel.value)
}

// 加载模型列表
const loadModelList = async () => {
  loading.value = true
  try {
    const response = await getModelList()
    if (response.code === 200) {
      modelList.value = response.data || []
    }
  } catch (error) {
    console.error('加载模型列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 监听外部 modelName 变化
watch(() => props.modelName, (newVal) => {
  selectedModel.value = newVal
})

onMounted(() => {
  loadModelList()
})
</script>

<style scoped>
.input-group {
  margin-bottom: 0;
}
</style>

