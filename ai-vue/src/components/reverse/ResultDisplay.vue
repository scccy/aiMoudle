<template>
  <div>
    <div class="card-title" style="margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;">
      <span>生成的配置（可编辑）</span>
      <div>
        <button class="btn btn-primary" style="padding: 5px 15px; font-size: 12px; margin-right: 10px;" @click="handleSave">
          保存配置
        </button>
        <button class="btn btn-success" style="padding: 5px 15px; font-size: 12px;" @click="handleConfirmSave" :disabled="saving">
          {{ saving ? '保存中...' : '确认保存到数据库' }}
        </button>
      </div>
    </div>
    
    <!-- 左右分栏布局 -->
    <div class="grid" style="grid-template-columns: 1fr 1fr; gap: 20px;">
      <!-- 左侧：Header Items 结果 -->
      <div class="card" style="margin-bottom: 0;">
        <div class="card-title" style="font-size: 16px; margin-bottom: 12px;">HeaderItem 配置</div>
        <div class="result-scroll-container">
          <div v-if="editableHeaderItems && editableHeaderItems.length > 0">
            <div v-for="(item, index) in editableHeaderItems" :key="index" class="result-item-editable">
              <ConfigItemEditor
                :item="item"
                key-placeholder="例如: contentTypeHeader"
                node-placeholder="例如: Content-Type"
                post-param-placeholder="例如: Content-Type"
                default-value-placeholder="例如: application/json"
                @update:item="(updatedItem) => updateItem('header', index, updatedItem)"
              />
            </div>
          </div>
          <div v-else class="empty-result">
            暂无 HeaderItem 配置
          </div>
        </div>
      </div>

      <!-- 右侧：Param Items 结果 -->
      <div class="card" style="margin-bottom: 0;">
        <div class="card-title" style="font-size: 16px; margin-bottom: 12px;">ParamItem 配置</div>
        <div class="result-scroll-container">
          <div v-if="editableParamItems && editableParamItems.length > 0">
            <div v-for="(item, index) in editableParamItems" :key="index" class="result-item-editable">
              <ConfigItemEditor
                :item="item"
                key-placeholder="例如: prompt"
                node-placeholder="例如: content[0].text"
                post-param-placeholder="例如: text"
                default-value-placeholder="例如: text"
                @update:item="(updatedItem) => updateItem('param', index, updatedItem)"
              />
            </div>
          </div>
          <div v-else class="empty-result">
            暂无 ParamItem 配置
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import ConfigItemEditor from './ConfigItemEditor.vue'

const props = defineProps({
  paramItems: {
    type: Array,
    default: () => []
  },
  headerItems: {
    type: Array,
    default: () => []
  },
  formData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:paramItems', 'update:headerItems', 'save', 'confirm-save'])

const saving = ref(false)

// 创建可编辑的副本
const editableParamItems = ref([])
const editableHeaderItems = ref([])

// 初始化可编辑数据
const initEditableData = () => {
  editableParamItems.value = props.paramItems.map(item => ({
    key: item.key || '',
    category: item.category || 'key',
    node: item.node || '',
    postParam: item.postParam || '',
    valueObject: item.valueObject || 'string',
    defaultValue: item.defaultValue || '',
    spelTemp: item.spelTemp || '',
    validate: item.validate 
      ? (typeof item.validate === 'string' ? item.validate : JSON.stringify(item.validate, null, 2))
      : ''
  }))
  
  editableHeaderItems.value = props.headerItems.map(item => ({
    key: item.key || '',
    category: item.category || 'key',
    node: item.node || '',
    postParam: item.postParam || '',
    valueObject: item.valueObject || 'string',
    defaultValue: item.defaultValue || '',
    spelTemp: item.spelTemp || '',
    validate: item.validate 
      ? (typeof item.validate === 'string' ? item.validate : JSON.stringify(item.validate, null, 2))
      : ''
  }))
}

// 监听 props 变化
watch(() => props.paramItems, () => {
  initEditableData()
}, { immediate: true, deep: true })

watch(() => props.headerItems, () => {
  initEditableData()
}, { immediate: true, deep: true })

// 更新配置项
const updateItem = (type, index, updatedItem) => {
  if (type === 'param') {
    editableParamItems.value[index] = updatedItem
  } else if (type === 'header') {
    editableHeaderItems.value[index] = updatedItem
  }
}

import { convertItemsToSaveFormat } from '../../utils/configItemHelper.js'

// 保存配置
const handleSave = () => {
  const paramItems = convertItemsToSaveFormat(editableParamItems.value, true)
  const headerItems = convertItemsToSaveFormat(editableHeaderItems.value, true)
  
  emit('save', { paramItems, headerItems })
  alert('配置已保存！')
}

// 确认保存到数据库
const handleConfirmSave = () => {
  // validate 保持为字符串格式，后端期望字符串类型
  const paramItems = convertItemsToSaveFormat(editableParamItems.value, false)
  const headerItems = convertItemsToSaveFormat(editableHeaderItems.value, false)
  
  emit('confirm-save', { paramItems, headerItems })
}
</script>

<style scoped>
.result-scroll-container {
  max-height: 600px;
  overflow-y: auto;
  padding: 10px;
}

.result-item-editable {
  margin-bottom: 16px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.empty-result {
  text-align: center;
  padding: 40px;
  color: #999;
}
</style>
