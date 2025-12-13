<template>
  <div>
    <!-- 校验规则选择 - 下拉框 -->
    <div style="margin-bottom: 12px;">
      <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">校验规则 (可选)</label>
      <select 
        :value="validateType"
        @change="handleTypeChange"
        style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; background: white;"
      >
        <option value="">无校验</option>
        <option value="maxLength">长度限制</option>
        <option value="enum">枚举限制</option>
        <option value="range">数值范围</option>
      </select>
    </div>
    
    <!-- 校验规则输入 -->
    <div v-if="validateType === 'maxLength'" style="margin-bottom: 8px;">
      <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">最大长度</label>
      <input 
        type="number" 
        :value="validateValue"
        @input="handleMaxLengthChange"
        placeholder="例如: 2000"
        style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
      />
    </div>
    <div v-if="validateType === 'enum'" style="margin-bottom: 8px;">
      <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">枚举值 (用逗号分隔)</label>
      <input 
        type="text" 
        :value="enumValue"
        @input="handleEnumChange"
        placeholder="例如: text,image_url"
        style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
      />
    </div>
    <div v-if="validateType === 'range'" style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 8px;">
      <div>
        <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">最小值</label>
        <input 
          type="number" 
          step="0.1"
          :value="rangeMin"
          @input="handleRangeChange($event.target.value, rangeMax)"
          placeholder="例如: 0.0"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
        />
      </div>
      <div>
        <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">最大值</label>
        <input 
          type="number" 
          step="0.1"
          :value="rangeMax"
          @input="handleRangeChange(rangeMin, $event.target.value)"
          placeholder="例如: 2.0"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  validate: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:validate'])

// 获取校验类型
const validateType = computed(() => {
  if (!props.validate) return ''
  try {
    const validate = JSON.parse(props.validate)
    if (validate.maxLength !== undefined) return 'maxLength'
    if (validate.enum !== undefined) return 'enum'
    if (validate.range !== undefined) return 'range'
  } catch (e) {
    return ''
  }
  return ''
})

// 获取校验值
const validateValue = computed(() => {
  if (!props.validate) return ''
  try {
    const validate = JSON.parse(props.validate)
    return validate.maxLength || ''
  } catch (e) {
    return ''
  }
})

// 获取枚举值（字符串格式）
const enumValue = computed(() => {
  if (!props.validate) return ''
  try {
    const validate = JSON.parse(props.validate)
    if (validate.enum && Array.isArray(validate.enum)) {
      return validate.enum.join(',')
    }
  } catch (e) {
    return ''
  }
  return ''
})

// 获取范围值
const rangeMin = computed(() => {
  if (!props.validate) return ''
  try {
    const validate = JSON.parse(props.validate)
    if (validate.range && Array.isArray(validate.range)) {
      return validate.range[0] || ''
    }
  } catch (e) {
    return ''
  }
  return ''
})

const rangeMax = computed(() => {
  if (!props.validate) return ''
  try {
    const validate = JSON.parse(props.validate)
    if (validate.range && Array.isArray(validate.range)) {
      return validate.range[1] || ''
    }
  } catch (e) {
    return ''
  }
  return ''
})

// 处理类型变化
const handleTypeChange = (event) => {
  const type = event.target.value
  if (!type || type === '') {
    emit('update:validate', '')
  } else if (type === 'maxLength') {
    emit('update:validate', JSON.stringify({ maxLength: 2000 }))
  } else if (type === 'enum') {
    emit('update:validate', JSON.stringify({ enum: [] }))
  } else if (type === 'range') {
    emit('update:validate', JSON.stringify({ range: [0.0, 2.0] }))
  }
}

// 处理最大长度变化
const handleMaxLengthChange = (event) => {
  const value = Number(event.target.value)
  emit('update:validate', JSON.stringify({ maxLength: value }))
}

// 处理枚举变化
const handleEnumChange = (event) => {
  const value = event.target.value
  const enumArray = value.split(',').map(item => item.trim()).filter(item => item)
  emit('update:validate', JSON.stringify({ enum: enumArray }))
}

// 处理范围变化
const handleRangeChange = (min, max) => {
  emit('update:validate', JSON.stringify({ range: [Number(min) || 0, Number(max) || 0] }))
}
</script>

