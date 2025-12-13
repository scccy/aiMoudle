<template>
  <div class="card">
    <div class="card-title">Header Mapping (请求头字段映射)</div>
    
    <div style="display: flex; justify-content: flex-end; margin-bottom: 12px;">
      <button class="btn btn-success" style="padding: 5px 10px; font-size: 12px;" @click="addHeaderMapping">
        + 添加
      </button>
    </div>
    
    <!-- 滚动容器：超过4个时显示滚动条 -->
    <div class="mapping-scroll-container">
      <div v-for="(item, index) in headerMapping" :key="index" class="mapping-item">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px;">
          <div>
              <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">业务字段名 (key)</label>
            <input 
              v-model="item.key" 
              placeholder="例如: contentTypeHeader" 
              style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
            />
          </div>
          <div>
              <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">Header 字段名 (postParam)</label>
            <input 
              v-model="item.postParam" 
              placeholder="例如: Content-Type" 
              style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
            />
          </div>
        </div>
        
        <!-- 校验规则编辑器 -->
        <ValidateRuleEditor 
          :validate="item.validate"
          @update:validate="(val) => updateItemValidate(index, val)"
        />
        
        <button class="btn btn-danger" style="padding: 5px 10px; font-size: 12px;" @click="removeHeaderMapping(index)">
          删除
        </button>
      </div>
      <div v-if="headerMapping.length === 0" style="text-align: center; padding: 40px; color: #999;">
        在左侧输入 Request Headers 后会自动填充映射配置
      </div>
    </div>
  </div>
</template>

<script setup>
import { watch } from 'vue'
import ValidateRuleEditor from './ValidateRuleEditor.vue'

const props = defineProps({
  headerMapping: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:headerMapping'])

const addHeaderMapping = () => {
  const newMapping = [...(props.headerMapping || []), { key: '', postParam: '', validate: '' }]
  emit('update:headerMapping', newMapping)
}

const removeHeaderMapping = (index) => {
  const newMapping = [...(props.headerMapping || [])]
  newMapping.splice(index, 1)
  emit('update:headerMapping', newMapping)
}

// 更新校验规则
const updateItemValidate = (index, validate) => {
  const newMapping = [...(props.headerMapping || [])]
  newMapping[index].validate = validate
  emit('update:headerMapping', newMapping)
}

// 监听内部变化，同步到父组件
watch(() => props.headerMapping, (newVal) => {
  emit('update:headerMapping', newVal)
}, { deep: true })
</script>

