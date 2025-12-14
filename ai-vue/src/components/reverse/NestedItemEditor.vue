<template>
  <div v-if="show" class="nested-config-section">
    <div class="section-title">
      📦 配置列表项结构
      <span class="hint">（定义列表中每个对象包含哪些字段）</span>
    </div>
    
    <div v-for="(field, index) in nestedFields" :key="index" style="margin-bottom: 16px; padding: 12px; border: 1px solid #e0e0e0; border-radius: 4px; background: #fafafa;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <span style="font-weight: 500; color: #333;">字段 {{ index + 1 }}: {{ field.key || '未命名' }}</span>
        <button 
          class="btn btn-danger" 
          style="padding: 4px 8px; font-size: 11px;"
          @click="removeField(index)"
        >
          删除
        </button>
      </div>
      
      <ConfigItemEditor
        :item="getNestedItem(field)"
        :key-placeholder="'例如: type'"
        :node-placeholder="'例如: type'"
        :post-param-placeholder="'例如: type'"
        :default-value-placeholder="'例如: image_url'"
        :request-body="{}"
        :nested="true"
        @update:item="(updatedItem) => updateNestedField(index, updatedItem)"
      />
    </div>
    
    <button 
      class="btn btn-success" 
      style="padding: 6px 12px; font-size: 12px; margin-top: 12px;"
      @click="addField"
    >
      + 添加字段
    </button>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import ConfigItemEditor from './ConfigItemEditor.vue'

const props = defineProps({
  spelTemp: {
    type: String,
    default: ''
  },
  show: {
    type: Boolean,
    default: false
  },
  node: {
    type: String,
    default: ''
  },
  requestBody: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:spelTemp'])

const nestedFields = ref([])
// 用于跟踪是否正在内部更新，避免 watch 循环
const isInternalUpdate = ref(false)

// 将嵌套字段转换为 ConfigItemEditor 需要的格式
const getNestedItem = (field) => {
  return {
    key: field.key || '',
    category: field.category || 'key', // 嵌套字段默认是 key 类型，但可以从数据中读取
    node: field.node || '',
    postParam: field.postParam || '',
    valueObject: field.valueObject || 'string',
    defaultValue: field.defaultValue || '',
    spelTemp: field.spelTemp || '', // 嵌套字段也可以有 spel_temp（当 value_object 是 list<json> 时）
    validate: field.validate || ''
  }
}

// 更新嵌套字段
const updateNestedField = (index, updatedItem) => {
  nestedFields.value[index] = {
    key: updatedItem.key || '',
    category: updatedItem.category || 'key',
    node: updatedItem.node || '',
    postParam: updatedItem.postParam || '',
    valueObject: updatedItem.valueObject || 'string',
    defaultValue: updatedItem.defaultValue || '',
    spelTemp: updatedItem.spelTemp || '', // 嵌套字段也可以有 spel_temp
    validate: updatedItem.validate || ''
  }
  // 只有当 key 不为空时才更新，避免空字段导致 watch 重置
  const hasValidKey = nestedFields.value.some(field => field.key && field.key.trim())
  if (hasValidKey || updatedItem.key) {
    updateNestedFields()
  }
}

// 解析 spel_temp 为嵌套字段数组
const parseSpelTemp = (spelTempStr) => {
  if (!spelTempStr || !spelTempStr.trim()) {
    return []
  }
  try {
    const config = JSON.parse(spelTempStr)
    const paramItems = config.paramItem || []
    return paramItems.map(item => ({
      key: item.key || '',
      category: item.category || 'key', // 嵌套字段默认是 key 类型
      node: item.node || '',
      postParam: item.post_param || '',
      defaultValue: item.default_value || '',
      valueObject: item.value_object || 'string',
      spelTemp: item.spel_temp || '', // 嵌套字段也可以有 spel_temp
      validate: item.validate || ''
    }))
  } catch (e) {
    console.error('解析 spel_temp 失败:', e)
    return []
  }
}

// 将嵌套字段数组转换为 spel_temp JSON 字符串
const buildSpelTemp = (fields) => {
  const paramItem = fields
    .filter(field => field.key && field.key.trim()) // 过滤掉空的字段
    .map(field => {
      // spel_temp 中的 node 使用相对路径（相对于父级 node）
      // 例如：如果字段路径是 "type"，则 node 为 "type"
      // 如果字段路径是 "image_url.url"，则 node 为 "image_url.url"
      // 在 PayloadEngine 处理时，会将父级 node（如 "content"）与这个相对路径组合
      const nodeValue = field.node || field.key
      
      const item = {
        key: field.key,
        category: field.category || 'key', // 嵌套字段默认是 key 类型
        node: nodeValue,
        post_param: field.postParam || field.key,
        value_object: field.valueObject || 'string'
      }
      if (field.defaultValue && field.defaultValue.trim()) {
        item.default_value = field.defaultValue.trim()
      }
      if (field.validate && field.validate.trim()) {
        item.validate = field.validate.trim()
      }
      if (field.spelTemp && field.spelTemp.trim()) {
        item.spel_temp = field.spelTemp.trim()
      }
      return item
    })
  
  if (paramItem.length === 0) {
    return ''
  }
  
  return JSON.stringify({ paramItem }, null, 2)
}


// 初始化：从 spel_temp 解析
watch(() => props.spelTemp, (newVal) => {
  // 如果是内部更新触发的，不重置 nestedFields
  if (isInternalUpdate.value) {
    return
  }
  
  if (props.show) {
    const newFields = parseSpelTemp(newVal)
    // 检查是否有正在编辑的字段（key 为空），如果有则保留
    const hasEmptyKey = nestedFields.value.some(field => !field.key || !field.key.trim())
    if (hasEmptyKey) {
      // 合并而不是替换，保留正在编辑的字段
      const existingEmptyFields = nestedFields.value.filter(field => !field.key || !field.key.trim())
      nestedFields.value = [...newFields, ...existingEmptyFields]
    } else {
      nestedFields.value = newFields
    }
    
    // 如果没有字段，添加一个空字段
    if (nestedFields.value.length === 0) {
      nestedFields.value = [{
        key: '',
        category: 'key',
        node: '',
        postParam: '',
        defaultValue: '',
        valueObject: 'string',
        spelTemp: '',
        validate: ''
      }]
    }
  }
}, { immediate: true })

// 监听 show 变化
watch(() => props.show, (newVal) => {
  if (newVal && !isInternalUpdate.value) {
    nestedFields.value = parseSpelTemp(props.spelTemp)
    if (nestedFields.value.length === 0) {
      nestedFields.value = [{
        key: '',
        category: 'key',
        node: '',
        postParam: '',
        defaultValue: '',
        valueObject: 'string',
        spelTemp: '',
        validate: ''
      }]
    }
  }
})

// 更新嵌套字段时，同步到 spel_temp
const updateNestedFields = () => {
  isInternalUpdate.value = true
  const spelTempStr = buildSpelTemp(nestedFields.value)
  emit('update:spelTemp', spelTempStr)
  // 使用 nextTick 确保 emit 完成后再重置标志
  nextTick(() => {
    isInternalUpdate.value = false
  })
}

// 添加字段
const addField = () => {
  nestedFields.value.push({
    key: '',
    category: 'key',
    node: '',
    postParam: '',
    defaultValue: '',
    valueObject: 'string',
    spelTemp: '',
    validate: ''
  })
  // 不立即调用 updateNestedFields，等用户输入 key 后再更新
  // 这样可以避免 watch 清空刚添加的字段
}

// 删除字段
const removeField = (index) => {
  nestedFields.value.splice(index, 1)
  updateNestedFields()
}
</script>

<style scoped>
.nested-config-section {
  margin-top: 16px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.section-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 12px;
  color: #333;
}

.hint {
  font-size: 12px;
  font-weight: normal;
  color: #666;
}

.nested-field-item {
  margin-bottom: 16px;
  padding: 12px;
  background: white;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
}

.field-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: bold;
  color: #333;
}

.field-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-row label {
  font-size: 12px;
  color: #666;
}
</style>

