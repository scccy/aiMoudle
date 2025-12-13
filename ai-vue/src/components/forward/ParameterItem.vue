<template>
  <div class="parameter-item">
    <div class="parameter-item-header">
      <input
        v-model="localKey"
        @input="updateKey"
        placeholder="参数名"
        class="param-key-input"
        style="flex: 1; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; margin-right: 8px;"
      />
      <select
        v-model="localType"
        @change="handleTypeChange"
        style="width: 120px; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; margin-right: 8px;"
      >
        <option value="string">字符串</option>
        <option value="number">数字</option>
        <option value="boolean">布尔值</option>
        <option value="list">列表</option>
        <option value="map">对象</option>
      </select>
      <button 
        class="btn btn-danger" 
        style="padding: 8px 12px; font-size: 12px;"
        @click="$emit('remove')"
      >
        删除
      </button>
    </div>
    
    <div class="parameter-item-body">
      <!-- 字符串类型 -->
      <div v-if="localType === 'string'" class="param-value-input">
        <textarea
          v-model="stringValue"
          @input="updateValue"
          placeholder="请输入字符串值"
          rows="3"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
        ></textarea>
      </div>
      
      <!-- 数字类型 -->
      <div v-if="localType === 'number'" class="param-value-input">
        <input
          v-model.number="numberValue"
          @input="updateValue"
          type="number"
          placeholder="请输入数字"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
        />
      </div>
      
      <!-- 布尔值类型 -->
      <div v-if="localType === 'boolean'" class="param-value-input">
        <select
          v-model="booleanValue"
          @change="updateValue"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
        >
          <option :value="true">true</option>
          <option :value="false">false</option>
        </select>
      </div>
      
      <!-- 列表类型 -->
      <div v-if="localType === 'list'" class="param-value-input">
        <ParameterList
          :value="listValue"
          @update:value="updateListValue"
        />
      </div>
      
      <!-- Map 类型 -->
      <div v-if="localType === 'map'" class="param-value-input">
        <ParameterMap
          :value="mapValue"
          @update:value="updateMapValue"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import ParameterList from './ParameterList.vue'
import ParameterMap from './ParameterMap.vue'

const props = defineProps({
  keyProp: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'string'
  },
  value: {
    type: [String, Number, Boolean, Array, Object],
    default: null
  }
})

const emit = defineEmits(['update:key', 'update:type', 'update:value', 'remove'])

const localKey = ref(props.keyProp)
const localType = ref(props.type)
const stringValue = ref(props.type === 'string' ? (props.value || '') : '')
const numberValue = ref(props.type === 'number' ? (props.value || 0) : 0)
const booleanValue = ref(props.type === 'boolean' ? (props.value !== undefined ? props.value : true) : true)
const listValue = ref(props.type === 'list' ? (props.value || []) : [])
const mapValue = ref(props.type === 'map' ? (props.value || {}) : {})

const updateKey = () => {
  emit('update:key', localKey.value)
}

const handleTypeChange = () => {
  emit('update:type', localType.value)
  // 类型变化时重置值
  if (localType.value === 'string') {
    stringValue.value = ''
    emit('update:value', '')
  } else if (localType.value === 'number') {
    numberValue.value = 0
    emit('update:value', 0)
  } else if (localType.value === 'boolean') {
    booleanValue.value = true
    emit('update:value', true)
  } else if (localType.value === 'list') {
    listValue.value = []
    emit('update:value', [])
  } else if (localType.value === 'map') {
    mapValue.value = {}
    emit('update:value', {})
  }
}

const updateValue = () => {
  if (localType.value === 'string') {
    emit('update:value', stringValue.value)
  } else if (localType.value === 'number') {
    emit('update:value', numberValue.value)
  } else if (localType.value === 'boolean') {
    emit('update:value', booleanValue.value)
  }
}

const updateListValue = (newValue) => {
  listValue.value = newValue
  emit('update:value', newValue)
}

const updateMapValue = (newValue) => {
  mapValue.value = newValue
  emit('update:value', newValue)
}

// 监听外部 props 变化
watch(() => props.keyProp, (newVal) => {
  localKey.value = newVal
})

watch(() => props.type, (newVal) => {
  localType.value = newVal
})

watch(() => props.value, (newVal) => {
  if (props.type === 'string') {
    stringValue.value = newVal || ''
  } else if (props.type === 'number') {
    numberValue.value = newVal || 0
  } else if (props.type === 'boolean') {
    booleanValue.value = newVal !== undefined ? newVal : true
  } else if (props.type === 'list') {
    listValue.value = newVal || []
  } else if (props.type === 'map') {
    mapValue.value = newVal || {}
  }
}, { deep: true })
</script>

<style scoped>
.parameter-item {
  margin-bottom: 16px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.parameter-item-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.parameter-item-body {
  margin-top: 8px;
}

.param-value-input {
  margin-top: 8px;
}
</style>

