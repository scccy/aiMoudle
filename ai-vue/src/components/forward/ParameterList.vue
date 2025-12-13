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
          <select
            v-model="item.type"
            @change="updateItemType(index, $event.target.value)"
            style="width: 120px; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; margin-right: 8px;"
          >
            <option value="string">字符串</option>
            <option value="number">数字</option>
            <option value="boolean">布尔值</option>
            <option value="map">对象</option>
          </select>
          <button 
            class="btn btn-danger" 
            style="padding: 6px 12px; font-size: 12px;"
            @click="removeItem(index)"
          >
            删除
          </button>
        </div>
        
        <div class="list-item-body">
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
  localValue.value.push({ type: 'string', value: '' })
  emitValue()
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
  margin-bottom: 12px;
  padding: 8px;
  background: #f9f9f9;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.list-item:last-child {
  margin-bottom: 0;
}

.list-item-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.list-item-body {
  margin-top: 8px;
}

.empty-list {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 12px;
}
</style>

