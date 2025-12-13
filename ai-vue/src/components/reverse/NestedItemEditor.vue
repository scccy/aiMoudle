<template>
  <div v-if="show" class="nested-config-section">
    <div class="section-title">
      📦 配置列表项结构
      <span class="hint">（定义列表中每个对象包含哪些字段）</span>
      <button 
        v-if="canAutoDetect"
        class="btn btn-primary" 
        style="padding: 4px 8px; font-size: 11px; margin-left: 12px;"
        @click="autoDetectFromRequestBody"
      >
        🔍 自动识别
      </button>
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
import { ref, watch, computed } from 'vue'
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
  updateNestedFields()
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

// 从 requestBody 中根据 node 路径提取数组
const extractArrayFromRequestBody = (nodePath) => {
  if (!nodePath || !props.requestBody || Object.keys(props.requestBody).length === 0) {
    return null
  }
  
  try {
    // 处理简单路径，如 "content"
    if (!nodePath.includes('[') && !nodePath.includes('.')) {
      const arr = props.requestBody[nodePath]
      if (Array.isArray(arr) && arr.length > 0) {
        return arr
      }
    }
    
    // 处理复杂路径，暂时只支持简单路径
    return null
  } catch (e) {
    console.error('提取数组失败:', e)
    return null
  }
}

// 检查是否可以自动识别
const canAutoDetect = computed(() => {
  if (!props.node || !props.requestBody || Object.keys(props.requestBody).length === 0) {
    return false
  }
  const arr = extractArrayFromRequestBody(props.node)
  return arr !== null && arr.length > 0 && typeof arr[0] === 'object' && arr[0] !== null
})

// 自动识别嵌套字段（分析所有对象，找出重复的字段结构）
const autoDetectFromRequestBody = () => {
  const arr = extractArrayFromRequestBody(props.node)
  if (!arr || arr.length === 0) {
    alert('无法从 Request Body 中找到对应的数组数据')
    return
  }
  
  // 过滤出所有对象类型的元素
  const objectItems = arr.filter(item => typeof item === 'object' && item !== null && !Array.isArray(item))
  if (objectItems.length === 0) {
    alert('数组元素不是对象类型，无法自动识别')
    return
  }
  
  // 收集所有对象的所有字段路径（包括嵌套字段）
  const allFieldPaths = new Map() // key: 字段路径, value: { count: 出现次数, values: 所有值, types: 所有类型 }
  
  objectItems.forEach(item => {
    collectFieldPaths(item, '', allFieldPaths)
  })
  
  // 找出在所有对象中都出现的字段（重复结构）
  const commonFields = []
  const totalObjects = objectItems.length
  
  for (const [fieldPath, fieldInfo] of allFieldPaths.entries()) {
    // 如果字段在所有对象中都出现，认为是重复结构
    if (fieldInfo.count === totalObjects) {
      const pathParts = fieldPath.split('.')
      const fieldName = pathParts[pathParts.length - 1]
      const nodePath = fieldPath
      
      // 推断默认值（如果所有值都相同）
      const uniqueValues = [...new Set(fieldInfo.values)]
      const defaultValue = uniqueValues.length === 1 ? uniqueValues[0] : ''
      
      // 推断类型（取最常见的类型）
      const typeCounts = {}
      fieldInfo.types.forEach(type => {
        typeCounts[type] = (typeCounts[type] || 0) + 1
      })
      const mostCommonType = Object.keys(typeCounts).reduce((a, b) => 
        typeCounts[a] > typeCounts[b] ? a : b
      )
      
      // spel_temp 中的 node 使用相对路径（相对于父级 node）
      // 例如：如果字段路径是 "type"，则 node 为 "type"
      // 如果字段路径是 "image_url.url"，则 node 为 "image_url.url"
      // 在 PayloadEngine 处理时，会将父级 node（如 "content"）与这个相对路径组合
      
      commonFields.push({
        key: fieldName,
        category: 'key', // 嵌套字段默认是 key 类型
        node: nodePath, // 直接使用相对路径
        postParam: fieldName,
        defaultValue: defaultValue,
        valueObject: mostCommonType,
        spelTemp: '', // 自动识别时不生成嵌套的 spel_temp
        validate: ''
      })
    }
  }
  
  if (commonFields.length === 0) {
    alert('未检测到所有对象共有的字段结构')
    return
  }
  
  // 按字段路径排序，保持层级关系
  commonFields.sort((a, b) => {
    const aDepth = a.node.split('.').length
    const bDepth = b.node.split('.').length
    if (aDepth !== bDepth) return aDepth - bDepth
    return a.node.localeCompare(b.node)
  })
  
  nestedFields.value = commonFields
  updateNestedFields()
}

// 递归收集字段路径
const collectFieldPaths = (obj, prefix, fieldPaths) => {
  if (typeof obj !== 'object' || obj === null || Array.isArray(obj)) {
    return
  }
  
  for (const [key, value] of Object.entries(obj)) {
    const currentPath = prefix ? `${prefix}.${key}` : key
    
    if (!fieldPaths.has(currentPath)) {
      fieldPaths.set(currentPath, {
        count: 0,
        values: [],
        types: []
      })
    }
    
    const fieldInfo = fieldPaths.get(currentPath)
    fieldInfo.count++
    
    // 处理嵌套对象
    if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
      collectFieldPaths(value, currentPath, fieldPaths)
    } else {
      // 记录值（用于推断默认值）
      if (value !== null && value !== undefined) {
        fieldInfo.values.push(value)
      }
      // 记录类型
      fieldInfo.types.push(inferValueObject(value))
    }
  }
}

// 推断值类型
const inferValueObject = (value) => {
  if (typeof value === 'string') return 'string'
  if (typeof value === 'number') {
    return Number.isInteger(value) ? 'int' : 'double'
  }
  return 'string'
}

// 推断默认值
const inferDefaultValue = (key, value) => {
  // 如果 key 是 type，且 value 是 image_url，返回 image_url
  if (key === 'type' && value === 'image_url') {
    return 'image_url'
  }
  // 如果 key 是 role，且 value 是 reference_image，返回 reference_image
  if (key === 'role' && value === 'reference_image') {
    return 'reference_image'
  }
  return ''
}

// 初始化：从 spel_temp 解析
watch(() => props.spelTemp, (newVal) => {
  if (props.show) {
    nestedFields.value = parseSpelTemp(newVal)
    // 如果没有字段且可以自动识别，提示用户
    if (nestedFields.value.length === 0 && canAutoDetect.value) {
      // 不自动填充，让用户点击按钮
    } else if (nestedFields.value.length === 0) {
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
  if (newVal) {
    nestedFields.value = parseSpelTemp(props.spelTemp)
    if (nestedFields.value.length === 0 && !canAutoDetect.value) {
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
  const spelTempStr = buildSpelTemp(nestedFields.value)
  emit('update:spelTemp', spelTempStr)
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
  updateNestedFields()
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

