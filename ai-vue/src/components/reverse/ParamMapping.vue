<template>
  <div class="card">
    <div class="card-title">Param Mapping (请求体字段映射)</div>
    
    <div style="display: flex; justify-content: flex-end; margin-bottom: 12px;">
      <button class="btn btn-success" style="padding: 5px 10px; font-size: 12px;" @click="addParamMapping">
        + 添加
      </button>
    </div>
    
    <!-- 滚动容器：超过4个时显示滚动条 -->
    <div class="mapping-scroll-container">
      <div v-for="(item, index) in paramMapping" :key="index" class="mapping-item">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px;">
          <div>
            <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">业务字段名 (key)</label>
            <input 
              v-model="item.key" 
              placeholder="例如: ratio" 
              style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
            />
          </div>
          <div>
            <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">目标字段名 (postParam)</label>
            <input 
              v-model="item.postParam" 
              placeholder="例如: text" 
              style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
            />
          </div>
        </div>
        
        <div style="margin-bottom: 12px;">
          <label style="display: block; margin-bottom: 4px; font-size: 12px; color: #666;">目标路径 (node) <span style="color: #999; font-weight: normal;">可选，不填则自动发现</span></label>
          <input 
            v-model="item.node" 
            placeholder="例如: content[0].text（用于模板拼接场景）" 
            style="width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;"
          />
        </div>
        
        <!-- 校验规则编辑器 -->
        <ValidateRuleEditor 
          :validate="item.validate"
          @update:validate="(val) => updateItemValidate(index, val)"
        />
        
        <button class="btn btn-danger" style="padding: 5px 10px; font-size: 12px;" @click="removeParamMapping(index)">
          删除
        </button>
      </div>
      <div v-if="paramMapping.length === 0" style="text-align: center; padding: 40px; color: #999;">
        <div style="margin-bottom: 8px;">Param Mapping 是可选的</div>
        <div style="font-size: 12px;">如果不填写，后端会自动从 Request Body 生成所有字段的映射</div>
        <div style="font-size: 12px; margin-top: 4px;">如果需要特殊参数（如 ratio），请手动添加映射关系</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { watch } from 'vue'
import ValidateRuleEditor from './ValidateRuleEditor.vue'

const props = defineProps({
  paramMapping: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:paramMapping'])

const addParamMapping = () => {
  const newMapping = [...(props.paramMapping || []), { key: '', postParam: '', node: '', validate: '' }]
  emit('update:paramMapping', newMapping)
}

const removeParamMapping = (index) => {
  const newMapping = [...(props.paramMapping || [])]
  newMapping.splice(index, 1)
  emit('update:paramMapping', newMapping)
}

// 更新校验规则
const updateItemValidate = (index, validate) => {
  const newMapping = [...(props.paramMapping || [])]
  newMapping[index].validate = validate
  emit('update:paramMapping', newMapping)
}

// 监听内部变化，同步到父组件
watch(() => props.paramMapping, (newVal) => {
  emit('update:paramMapping', newVal)
}, { deep: true })
</script>

