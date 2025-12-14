<template>
  <div class="parameter-list">
    <div class="list-header">
      <span style="font-size: 12px; color: #666;">列表项（可添加字符串、数字、布尔值、对象）</span>
      <button 
        class="btn btn-primary" 
        style="padding: 6px 12px; font-size: 12px;"
        @click="addItem"
      >
        + 添加项
      </button>
    </div>
    
    <div class="list-items">
      <div v-for="(item, index) in localValue" :key="index" class="list-item">
        <div class="list-item-header">
          <div style="display: flex; align-items: center; gap: 12px;">
            <button
              class="collapse-btn"
              @click="toggleCollapse(index)"
              :title="collapsedItems[index] ? '展开' : '收起'"
            >
              <span class="collapse-icon" :class="{ collapsed: collapsedItems[index] }">▼</span>
            </button>
            <span class="item-index-badge">项 {{ index + 1 }}</span>
            <select
              v-model="item.type"
              @change="updateItemType(index, $event.target.value)"
              style="width: 120px; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px;"
            >
              <option value="string">字符串</option>
              <option value="number">数字</option>
              <option value="boolean">布尔值</option>
              <option value="map">对象</option>
            </select>
          </div>
          <div style="display: flex; gap: 8px;">
            <button 
              class="btn btn-danger" 
              style="padding: 6px 12px; font-size: 12px;"
              @click="removeItem(index)"
            >
              删除
            </button>
          </div>
        </div>
        
        <div v-show="!collapsedItems[index]" class="list-item-body">
          <!-- 字符串类型 -->
          <textarea
            v-if="item.type === 'string'"
            v-model="item.value"
            @input="updateItemValue(index)"
            placeholder="请输入字符串值"
            rows="2"
            style="width: 100%; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; margin-top: 8px;"
          ></textarea>
          
          <!-- 数字类型 -->
          <input
            v-if="item.type === 'number'"
            v-model.number="item.value"
            @input="updateItemValue(index)"
            type="number"
            placeholder="请输入数字"
            style="width: 100%; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; margin-top: 8px;"
          />
          
          <!-- 布尔值类型 -->
          <select
            v-if="item.type === 'boolean'"
            v-model="item.value"
            @change="updateItemValue(index)"
            style="width: 100%; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; margin-top: 8px;"
          >
            <option :value="true">true</option>
            <option :value="false">false</option>
          </select>
          
          <!-- Map 类型 -->
          <div v-if="item.type === 'map'" style="margin-top: 8px;">
            <ParameterMap
              :value="item.value"
              @update:value="(val) => updateMapItem(index, val)"
            />
          </div>
        </div>
      </div>
      
      <div v-if="localValue.length === 0" class="empty-list">
        暂无列表项，点击"添加项"开始添加
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import ParameterMap from './ParameterMap.vue'

const props = defineProps({
  value: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:value'])

// 跟踪每个列表项的收起/展开状态
const collapsedItems = ref({})

const toggleCollapse = (index) => {
  collapsedItems.value[index] = !collapsedItems.value[index]
}

const localValue = ref(props.value.map(item => {
  // 推断类型并包装
  if (typeof item === 'string') {
    return { type: 'string', value: item }
  } else if (typeof item === 'number') {
    return { type: 'number', value: item }
  } else if (typeof item === 'boolean') {
    return { type: 'boolean', value: item }
  } else if (typeof item === 'object' && item !== null && !Array.isArray(item)) {
    return { type: 'map', value: item }
  } else {
    return { type: 'string', value: String(item) }
  }
}))

const addItem = () => {
  // 检查列表中是否已有元素，如果有且是对象类型，则添加类似结构的对象
  if (localValue.value.length > 0) {
    const firstItem = localValue.value[0]
    if (firstItem.type === 'map' && firstItem.value && typeof firstItem.value === 'object') {
      // 如果第一个元素是对象，创建一个类似结构的空对象
      const newObject = createEmptyObjectFromTemplate(firstItem.value)
      localValue.value.push({ type: 'map', value: newObject })
    } else {
      // 否则添加默认类型
      localValue.value.push({ type: 'string', value: '' })
    }
  } else {
    // 列表为空，添加默认类型
    localValue.value.push({ type: 'string', value: '' })
  }
  emitValue()
}

// 根据模板对象创建空对象（保持结构）
const createEmptyObjectFromTemplate = (template) => {
  if (!template || typeof template !== 'object' || Array.isArray(template)) {
    return {}
  }
  
  const newObject = {}
  for (const [key, value] of Object.entries(template)) {
    if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
      // 嵌套对象，递归创建
      newObject[key] = createEmptyObjectFromTemplate(value)
    } else if (Array.isArray(value)) {
      // 数组，创建空数组
      newObject[key] = []
    } else if (typeof value === 'string') {
      // 字符串，创建空字符串
      newObject[key] = ''
    } else if (typeof value === 'number') {
      // 数字，创建 0
      newObject[key] = 0
    } else if (typeof value === 'boolean') {
      // 布尔值，创建 false
      newObject[key] = false
    } else {
      // 其他类型，创建空字符串
      newObject[key] = ''
    }
  }
  return newObject
}

const removeItem = (index) => {
  localValue.value.splice(index, 1)
  emitValue()
}

const updateItemType = (index, newType) => {
  const item = localValue.value[index]
  item.type = newType
  // 重置值
  if (newType === 'string') {
    item.value = ''
  } else if (newType === 'number') {
    item.value = 0
  } else if (newType === 'boolean') {
    item.value = true
  } else if (newType === 'map') {
    item.value = {}
  }
  emitValue()
}

const updateItemValue = (index) => {
  emitValue()
}

const updateMapItem = (index, mapValue) => {
  localValue.value[index].value = mapValue
  emitValue()
}

const emitValue = () => {
  // 转换为实际值数组
  const actualValue = localValue.value.map(item => item.value)
  emit('update:value', actualValue)
}

// 监听外部 value 变化
watch(() => props.value, (newVal) => {
  if (Array.isArray(newVal)) {
    const oldLength = localValue.value.length
    localValue.value = newVal.map(item => {
      if (typeof item === 'string') {
        return { type: 'string', value: item }
      } else if (typeof item === 'number') {
        return { type: 'number', value: item }
      } else if (typeof item === 'boolean') {
        return { type: 'boolean', value: item }
      } else if (typeof item === 'object' && item !== null && !Array.isArray(item)) {
        return { type: 'map', value: item }
      } else {
        return { type: 'string', value: String(item) }
      }
    })
    // 如果列表项数量减少，清理对应的收起状态
    if (newVal.length < oldLength) {
      const newCollapsed = {}
      for (let i = 0; i < newVal.length; i++) {
        if (collapsedItems.value[i] !== undefined) {
          newCollapsed[i] = collapsedItems.value[i]
        }
      }
      collapsedItems.value = newCollapsed
    }
  }
}, { deep: true })
</script>

<style scoped>
.parameter-list {
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 12px;
  background: white;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e0e0e0;
}

.list-items {
  min-height: 40px;
}

.list-item {
  margin-bottom: 16px;
  padding: 16px;
  background: #ffffff;
  border-radius: 6px;
  border: 2px solid #e0e0e0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.list-item:hover {
  border-color: #d0d0d0;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.list-item:last-child {
  margin-bottom: 0;
}

.list-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.list-item-body {
  margin-top: 12px;
  padding-top: 12px;
}

.item-index-badge {
  display: inline-block;
  padding: 4px 10px;
  background: #f0f0f0;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  border: 1px solid #e0e0e0;
}

.collapse-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.collapse-btn:hover {
  background: #f0f0f0;
}

.collapse-icon {
  display: inline-block;
  font-size: 12px;
  color: #666;
  transition: transform 0.2s ease;
  user-select: none;
}

.collapse-icon.collapsed {
  transform: rotate(-90deg);
}

.list-item-body {
  margin-top: 12px;
  padding-top: 12px;
  animation: slideDown 0.2s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 1000px;
  }
}

.empty-list {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 12px;
}
</style>

