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
        <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">处理类型 (category)</label>
        <select 
          :value="item.category"
          @change="updateField('category', $event.target.value)"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; background: white;"
        >
          <option value="key">key - 普通字段</option>
          <option value="map">map - 对象</option>
          <option value="list">list - 列表</option>
        </select>
      </div>
    </div>
    
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 8px;">
      <div>
        <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">目标路径 (node)</label>
        <input 
          :value="item.node"
          @input="updateField('node', $event.target.value)"
          :placeholder="nodePlaceholder"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
        />
      </div>
      <div>
        <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">目标字段名 (post_param)</label>
        <input 
          :value="item.postParam"
          @input="updateField('postParam', $event.target.value)"
          :placeholder="postParamPlaceholder"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
        />
      </div>
    </div>
    
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 8px;">
      <div>
        <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">类型 (value_object)</label>
        <select 
          :value="item.valueObject"
          @change="updateField('valueObject', $event.target.value)"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; background: white;"
        >
          <option value="string">string - 字符串</option>
          <option value="int">int - 整数</option>
          <option value="double">double - 浮点数</option>
          <option :value="'list<string>'">list&lt;string&gt; - 字符串列表</option>
          <option :value="'list<json>'">list&lt;json&gt; - JSON列表</option>
          <option value="map">map - 对象</option>
        </select>
      </div>
      <div>
        <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">默认值 (default_value)</label>
        <input 
          :value="item.defaultValue"
          @input="updateField('defaultValue', $event.target.value)"
          :placeholder="defaultValuePlaceholder"
          style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
        />
      </div>
    </div>
    
    <div style="margin-bottom: 8px;">
      <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">字符串模板 (spel_temp) <span style="color: #999; font-weight: normal;">可选</span></label>
      <input 
        :value="item.spelTemp"
        @input="updateField('spelTemp', $event.target.value)"
        placeholder="例如: Bearer {value} 或  --ratio {value}"
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
  }
})

const emit = defineEmits(['update:item'])

// 获取校验规则的字符串格式
const getValidateString = (validate) => {
  if (!validate) return ''
  if (typeof validate === 'string') return validate
  return JSON.stringify(validate, null, 2)
}

// 更新字段
const updateField = (field, value) => {
  const updatedItem = { ...props.item, [field]: value }
  emit('update:item', updatedItem)
}
</script>

