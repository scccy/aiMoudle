<template>
  <div class="card">
    <div class="card-title">请求参数</div>
    
    <div class="parameter-form">
      <div v-for="(param, index) in parameters" :key="index" class="parameter-wrapper">
        <ParameterItem
          :key="param.id"
          :key-prop="param.key"
          :type="param.type"
          :value="param.value"
          @update:key="(key) => updateParamKey(index, key)"
          @update:type="(type) => updateParamType(index, type)"
          @update:value="(value) => updateParamValue(index, value)"
          @remove="removeParameter(index)"
        />
      </div>
      
      <div v-if="parameters.length === 0" class="empty-parameters">
        <p style="color: #999; text-align: center; padding: 40px;">
          暂无参数，点击"添加参数"开始添加
        </p>
      </div>
      
      <div class="form-actions">
        <button 
          class="btn btn-primary" 
          @click="addParameter"
          style="width: 100%; margin-top: 16px;"
        >
          + 添加参数
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import ParameterItem from './ParameterItem.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue'])

// 参数列表，每个参数有唯一 ID
let paramIdCounter = 0
const parameters = ref([])

// 将对象转换为参数列表
const objectToParameters = (obj) => {
  if (!obj || typeof obj !== 'object') {
    return []
  }
  return Object.keys(obj).map(key => {
    const val = obj[key]
    let type = 'string'
    if (typeof val === 'number') {
      type = 'number'
    } else if (typeof val === 'boolean') {
      type = 'boolean'
    } else if (Array.isArray(val)) {
      type = 'list'
    } else if (typeof val === 'object' && val !== null) {
      type = 'map'
    }
    return {
      id: paramIdCounter++,
      key: key,
      type: type,
      value: val
    }
  })
}

// 初始化参数列表
const initParameters = () => {
  parameters.value = objectToParameters(props.modelValue)
}

const addParameter = () => {
  parameters.value.push({
    id: paramIdCounter++,
    key: '',
    type: 'string',
    value: ''
  })
  // 不立即 emitValue，等用户输入 key 后再 emit
  // 这样可以避免 watch 清空刚添加的参数
}

const removeParameter = (index) => {
  parameters.value.splice(index, 1)
  emitValue()
}

const updateParamKey = (index, key) => {
  parameters.value[index].key = key
  emitValue()
}

const updateParamType = (index, type) => {
  parameters.value[index].type = type
  // 重置值
  if (type === 'string') {
    parameters.value[index].value = ''
  } else if (type === 'number') {
    parameters.value[index].value = 0
  } else if (type === 'boolean') {
    parameters.value[index].value = true
  } else if (type === 'list') {
    parameters.value[index].value = []
  } else if (type === 'map') {
    parameters.value[index].value = {}
  }
  emitValue()
}

const updateParamValue = (index, value) => {
  parameters.value[index].value = value
  emitValue()
}

const emitValue = () => {
  // 转换为实际对象
  const actualValue = {}
  parameters.value.forEach(param => {
    if (param.key && param.key.trim()) {
      actualValue[param.key] = param.value
    }
  })
  emit('update:modelValue', actualValue)
}

// 监听外部 modelValue 变化
watch(() => props.modelValue, (newVal) => {
  if (newVal && typeof newVal === 'object' && !Array.isArray(newVal)) {
    const currentKeys = new Set(parameters.value.map(p => p.key).filter(k => k && k.trim()))
    const newKeys = new Set(Object.keys(newVal))
    
    // 检查是否有未完成的参数（key 为空）
    const hasIncompleteParams = parameters.value.some(p => !p.key || !p.key.trim())
    
    // 如果键集合不同且没有未完成的参数，重新初始化
    if (!hasIncompleteParams && 
        (currentKeys.size !== newKeys.size || 
         [...currentKeys].some(k => !newKeys.has(k)))) {
      initParameters()
    } else if (!hasIncompleteParams) {
      // 只更新值（当没有未完成的参数时）
      parameters.value.forEach(param => {
        if (param.key && param.key.trim() && newVal[param.key] !== undefined) {
          param.value = newVal[param.key]
        }
      })
    }
    // 如果有未完成的参数，不进行任何操作，让用户继续编辑
  } else if (!newVal || Object.keys(newVal).length === 0) {
    // 只有在没有任何参数时才清空（包括未完成的参数）
    const hasAnyParams = parameters.value.length > 0
    if (!hasAnyParams) {
      parameters.value = []
    }
  }
}, { deep: true })

// 初始化
initParameters()
</script>

<style scoped>
.parameter-form {
  min-height: 200px;
}

.parameter-wrapper {
  margin-bottom: 16px;
}

.empty-parameters {
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-actions {
  margin-top: 16px;
}
</style>

