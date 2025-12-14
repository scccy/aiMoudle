<template>
  <div class="parameter-map">
    <div class="map-header">
      <span style="font-size: 12px; color: #666;">对象属性（键值对）</span>
      <button 
        class="btn btn-primary" 
        style="padding: 6px 12px; font-size: 12px;"
        @click="addProperty"
      >
        + 添加属性
      </button>
    </div>
    
    <div class="map-properties">
      <div v-for="(prop, index) in properties" :key="index" class="map-property">
        <div class="property-row">
          <input
            v-model="prop.key"
            @input="updateProperty"
            placeholder="属性名"
            class="property-key"
            style="flex: 1; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; margin-right: 8px;"
          />
          <select
            v-model="prop.type"
            @change="updatePropertyType(index)"
            style="width: 120px; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; margin-right: 8px;"
          >
            <option value="string">字符串</option>
            <option value="number">数字</option>
            <option value="boolean">布尔值</option>
            <option value="list">列表</option>
            <option value="map">对象</option>
          </select>
          <button 
            class="btn btn-success" 
            style="padding: 6px 12px; font-size: 12px; margin-right: 8px;"
            @click="duplicateProperty(index)"
            title="添加相同组"
          >
            + 相同组
          </button>
          <button 
            class="btn btn-danger" 
            style="padding: 6px 12px; font-size: 12px;"
            @click="removeProperty(index)"
          >
            删除
          </button>
        </div>
        
        <div class="property-value">
          <!-- 字符串类型 -->
          <textarea
            v-if="prop.type === 'string'"
            v-model="prop.value"
            @input="updateProperty"
            placeholder="请输入字符串值"
            rows="2"
            style="width: 100%; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; margin-top: 8px;"
          ></textarea>
          
          <!-- 数字类型 -->
          <input
            v-if="prop.type === 'number'"
            v-model.number="prop.value"
            @input="updateProperty"
            type="number"
            placeholder="请输入数字"
            style="width: 100%; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; margin-top: 8px;"
          />
          
          <!-- 布尔值类型 -->
          <select
            v-if="prop.type === 'boolean'"
            v-model="prop.value"
            @change="updateProperty"
            style="width: 100%; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; margin-top: 8px;"
          >
            <option :value="true">true</option>
            <option :value="false">false</option>
          </select>
          
          <!-- 列表类型 -->
          <div v-if="prop.type === 'list'" style="margin-top: 8px;">
            <ParameterList
              :value="prop.value"
              @update:value="(val) => updateListProperty(index, val)"
            />
          </div>
          
          <!-- Map 类型（嵌套） -->
          <div v-if="prop.type === 'map'" style="margin-top: 8px;">
            <ParameterMap
              :value="prop.value"
              @update:value="(val) => updateMapProperty(index, val)"
            />
          </div>
        </div>
      </div>
      
      <div v-if="properties.length === 0" class="empty-map">
        暂无属性，点击"添加属性"开始添加
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import ParameterList from './ParameterList.vue'

const props = defineProps({
  value: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:value'])

// 将对象转换为属性数组
const objectToProperties = (obj) => {
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
    return { key, type, value: val }
  })
}

const properties = ref(objectToProperties(props.value))

// 用于跟踪是否正在内部更新，避免 watch 循环
const isInternalUpdate = ref(false)

const addProperty = () => {
  properties.value.push({ key: '', type: 'string', value: '' })
  // 不立即 emitValue，等用户输入 key 后再 emit
  // 这样可以避免 watch 清空刚添加的属性
}

const removeProperty = (index) => {
  properties.value.splice(index, 1)
  isInternalUpdate.value = true
  emitValue()
  nextTick(() => {
    isInternalUpdate.value = false
  })
}

const duplicateProperty = (index) => {
  const prop = properties.value[index]
  // 深拷贝属性值
  let duplicatedValue
  if (prop.type === 'list' && Array.isArray(prop.value)) {
    duplicatedValue = JSON.parse(JSON.stringify(prop.value))
  } else if (prop.type === 'map' && typeof prop.value === 'object' && prop.value !== null) {
    duplicatedValue = JSON.parse(JSON.stringify(prop.value))
  } else {
    duplicatedValue = prop.value
  }
  
  // 在当前位置后插入复制的属性
  properties.value.splice(index + 1, 0, {
    key: prop.key,
    type: prop.type,
    value: duplicatedValue
  })
  isInternalUpdate.value = true
  emitValue()
  nextTick(() => {
    isInternalUpdate.value = false
  })
}

const updateProperty = () => {
  isInternalUpdate.value = true
  emitValue()
  // 使用 nextTick 确保 emit 完成后再重置标志
  nextTick(() => {
    isInternalUpdate.value = false
  })
}

const updatePropertyType = (index) => {
  const prop = properties.value[index]
  // 重置值
  if (prop.type === 'string') {
    prop.value = ''
  } else if (prop.type === 'number') {
    prop.value = 0
  } else if (prop.type === 'boolean') {
    prop.value = true
  } else if (prop.type === 'list') {
    prop.value = []
  } else if (prop.type === 'map') {
    prop.value = {}
  }
  isInternalUpdate.value = true
  emitValue()
  nextTick(() => {
    isInternalUpdate.value = false
  })
}

const updateListProperty = (index, listValue) => {
  properties.value[index].value = listValue
  isInternalUpdate.value = true
  emitValue()
  nextTick(() => {
    isInternalUpdate.value = false
  })
}

const updateMapProperty = (index, mapValue) => {
  properties.value[index].value = mapValue
  isInternalUpdate.value = true
  emitValue()
  nextTick(() => {
    isInternalUpdate.value = false
  })
}

const emitValue = () => {
  // 转换为实际对象
  const actualValue = {}
  properties.value.forEach(prop => {
    if (prop.key && prop.key.trim()) {
      actualValue[prop.key] = prop.value
    }
  })
  emit('update:value', actualValue)
}

// 监听外部 value 变化
watch(() => props.value, (newVal) => {
  // 如果是内部更新触发的，不重置 properties
  if (isInternalUpdate.value) {
    return
  }
  
  if (newVal && typeof newVal === 'object' && !Array.isArray(newVal)) {
    const newProperties = objectToProperties(newVal)
    // 只有当新值和当前值不同时才更新
    // 检查是否有新增的空属性（key 为空），如果有则保留
    const hasEmptyKey = properties.value.some(prop => !prop.key || !prop.key.trim())
    if (hasEmptyKey) {
      // 如果有空 key 的属性，合并而不是替换
      const existingEmptyProps = properties.value.filter(prop => !prop.key || !prop.key.trim())
      properties.value = [...newProperties, ...existingEmptyProps]
    } else {
      properties.value = newProperties
    }
  } else if (!newVal || Object.keys(newVal || {}).length === 0) {
    // 如果新值为空，检查是否有正在编辑的属性（key 为空），如果有则保留
    const hasEmptyKey = properties.value.some(prop => !prop.key || !prop.key.trim())
    if (!hasEmptyKey) {
      properties.value = []
    }
  }
}, { deep: true })
</script>

<style scoped>
.parameter-map {
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 12px;
  background: white;
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e0e0e0;
}

.map-properties {
  min-height: 40px;
}

.map-property {
  margin-bottom: 12px;
  padding: 8px;
  background: #f9f9f9;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.map-property:last-child {
  margin-bottom: 0;
}

.property-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.property-value {
  margin-top: 8px;
}

.empty-map {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 12px;
}
</style>

