<template>
  <div style="margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid #eee;">
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 8px;">
      <div>
        <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">业务字段名 (key)</label>
        <input 
          :value="item.key"
          @input="updateField('key', $event.target.value)"
          :placeholder="keyPlaceholder"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
        />
      </div>
      <div>
        <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">目标路径 (node)</label>
        <input 
          :value="item.node"
          @input="updateField('node', $event.target.value)"
          :placeholder="nodePlaceholder"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
        />
      </div>
    </div>
    
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 8px;">
      <div>
        <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">目标字段名 (post_param)</label>
        <input 
          :value="item.postParam"
          @input="updateField('postParam', $event.target.value)"
          :placeholder="postParamPlaceholder"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
        />
      </div>
      <div>
        <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">类型 (value_object)</label>
        <select 
          v-model="localValueObject"
          @change="handleValueObjectChange"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; background: white;"
        >
          <!-- 基础类型 -->
          <option value="string">📝 文本 (string) - 单个文本值，示例: "hello world"</option>
          <option value="int">🔢 整数 (int) - 整数，示例: 100</option>
          <option value="long">🔢 长整数 (long) - 长整数，示例: 1000000000</option>
          <option value="double">🔢 小数 (double) - 小数，示例: 3.14</option>
          <option value="decimal">🔢 精确小数 (decimal) - 精确小数，示例: 0.5</option>
          <option value="boolean">✅ 布尔值 (boolean) - 布尔值，示例: true/false</option>
          <!-- 对象类型 -->
          <option value="map">📦 单个对象 (map) - 一个对象，示例: {"key": "value", "name": "..."}</option>
          <option value="json">📦 JSON对象 (json) - JSON对象，示例: {"key": "value"}</option>
          <!-- 列表类型 -->
          <option :value="'list<string>'">📋 文本列表 (list&lt;string&gt;) - 多个文本值，示例: ["文本1", "文本2"]</option>
          <option :value="'list<int>'">📋 整数列表 (list&lt;int&gt;) - 多个整数，示例: [1, 2, 3]</option>
          <option :value="'list<double>'">📋 小数列表 (list&lt;double&gt;) - 多个小数，示例: [1.1, 2.2, 3.3]</option>
          <option :value="'list<boolean>'">📋 布尔列表 (list&lt;boolean&gt;) - 多个布尔值，示例: [true, false]</option>
          <option :value="'list<json>'">📦 对象列表 (list&lt;json&gt;) - 多个对象，示例: [{"type": "text"}, {"url": "..."}]</option>
        </select>
      </div>
    </div>
    
    <div style="margin-bottom: 8px;">
      <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">默认值 (default_value)</label>
      <input 
        :value="item.defaultValue"
        @input="updateField('defaultValue', $event.target.value)"
        :placeholder="defaultValuePlaceholder"
        style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
      />
    </div>
    
    <!-- 字符串模板 (spel_temp) - 用于特殊情况，如 --xxx {value} -->
    <div v-if="item.valueObject !== 'list<json>'" style="margin-bottom: 8px;">
      <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">字符串模板 (spel_temp) <span style="color: #999; font-weight: normal;">可选</span></label>
      <input 
        :value="item.spelTemp"
        @input="updateField('spelTemp', $event.target.value)"
        placeholder="例如: Bearer {value} 或 --ratio {value}"
        style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
      />
    </div>
    
    <!-- 校验规则编辑器 -->
    <ValidateRuleEditor 
      :validate="getValidateString(item.validate)"
      @update:validate="(val) => updateField('validate', val)"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import ValidateRuleEditor from './ValidateRuleEditor.vue'

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  keyPlaceholder: {
    type: String,
    default: '例如: prompt'
  },
  nodePlaceholder: {
    type: String,
    default: '例如: content[0].text'
  },
  postParamPlaceholder: {
    type: String,
    default: '例如: text'
  },
  defaultValuePlaceholder: {
    type: String,
    default: '例如: text'
  },
  requestBody: {
    type: Object,
    default: () => ({})
  },
  nested: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:item'])

// 使用本地状态控制 select 的值
const localValueObject = ref(props.item.valueObject || 'string')

// 监听整个 item prop 的变化，同步所有字段
watch(() => props.item, (newItem) => {
  if (newItem) {
    localValueObject.value = newItem.valueObject || 'string'
  }
}, { deep: true, immediate: true })

// 监听 props.item.valueObject 变化，同步到本地状态
watch(() => props.item.valueObject, (newVal) => {
  localValueObject.value = newVal || 'string'
}, { immediate: true })

// 获取校验规则的字符串格式
const getValidateString = (validate) => {
  if (!validate) return ''
  if (typeof validate === 'string') return validate
  return JSON.stringify(validate, null, 2)
}

// 更新字段
const updateField = (field, value) => {
  // 构建更新后的项
  const updatedItem = { 
    ...props.item, 
    [field]: value
  }
  
  // 确保 category 字段被保留（如果字段不是 category）
  if (field !== 'category') {
    updatedItem.category = props.item.category || 'key'
  }
  
  // 确保 spelTemp 字段被保留（如果字段不是 spelTemp）
  if (field !== 'spelTemp') {
    updatedItem.spelTemp = props.item.spelTemp || ''
  }
  
  emit('update:item', updatedItem)
}

// 处理 value_object 变化
const handleValueObjectChange = () => {
  const newValue = localValueObject.value
  updateField('valueObject', newValue)
}
</script>

