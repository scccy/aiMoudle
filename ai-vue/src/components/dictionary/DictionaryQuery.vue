<template>
  <div class="dictionary-query-page">
    <div class="container">
      <h1 style="text-align: center; margin-bottom: 30px; color: #1a1a1a;">
        📚 字典映射查询
      </h1>

      <!-- 查询区域 -->
      <div class="card" style="margin-bottom: 20px;">
        <div class="card-title">查询条件</div>
        <div class="search-form">
          <div class="form-row">
            <label>模型名称:</label>
            <input 
              v-model="searchForm.modelName" 
              type="text"
              list="modelName-list"
              placeholder="请输入或选择模型名称"
              class="form-input"
            />
            <datalist id="modelName-list">
              <option v-for="modelName in options.modelNames" :key="modelName" :value="modelName">
                {{ modelName }}
              </option>
            </datalist>
          </div>
          <div class="form-row">
            <label>Key:</label>
            <input 
              v-model="searchForm.key" 
              type="text"
              list="key-list"
              placeholder="请输入或选择 Key"
              class="form-input"
            />
            <datalist id="key-list">
              <option v-for="key in options.keys" :key="key" :value="key">
                {{ key }}
              </option>
            </datalist>
          </div>
          <div class="form-row">
            <label>Post Param:</label>
            <input 
              v-model="searchForm.postParam" 
              type="text"
              list="postParam-list"
              placeholder="请输入或选择 Post Param"
              class="form-input"
            />
            <datalist id="postParam-list">
              <option v-for="postParam in options.postParams" :key="postParam" :value="postParam">
                {{ postParam }}
              </option>
            </datalist>
          </div>
          <div class="form-actions">
            <button class="btn btn-primary" @click="handleSearch()" :disabled="loading">
              {{ loading ? '查询中...' : '🔍 查询' }}
            </button>
            <button class="btn btn-secondary" @click="handleReset()" style="margin-left: 10px;">
              重置
            </button>
          </div>
        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="error" class="error">
        {{ error }}
      </div>

      <!-- 映射关系列表 -->
      <div class="card" style="margin-bottom: 20px;">
        <div class="card-title">
          映射关系列表
          <span class="count-badge" v-if="pagination.total > 0">
            (共 {{ pagination.total }} 条)
          </span>
        </div>
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="mappingList.length === 0" class="empty-state">
          暂无数据，请点击查询按钮
        </div>
        <div v-else>
          <div class="mapping-table">
            <table>
              <thead>
                <tr>
                  <th>模型名称</th>
                  <th>Key</th>
                  <th>Post Param</th>
                  <th>说明</th>
                  <th>Item ID</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr 
                  v-for="(item, index) in mappingList" 
                  :key="index"
                  :class="{ 'selected': selectedMapping && selectedMapping.itemId === item.itemId }"
                  @click="selectMapping(item)"
                >
                  <td>{{ item.modelName || '-' }}</td>
                  <td>{{ item.key || '-' }}</td>
                  <td>{{ item.postParam || '-' }}</td>
                  <td>{{ item.description || '-' }}</td>
                  <td>{{ item.itemId || '-' }}</td>
                  <td>
                    <button 
                      class="btn btn-sm btn-info" 
                      @click.stop="viewItemDetail(item)"
                    >
                      查看详情
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <!-- 分页组件 -->
          <div class="pagination-container">
            <div class="pagination-info">
              显示第 {{ (pagination.current - 1) * pagination.size + 1 }} - {{ Math.min(pagination.current * pagination.size, pagination.total) }} 条，共 {{ pagination.total }} 条
            </div>
            <div class="pagination-controls">
              <button 
                class="btn btn-sm btn-secondary" 
                :disabled="pagination.current <= 1 || loading"
                @click="() => handlePageChange(pagination.current - 1)"
              >
                上一页
              </button>
              <span class="page-info">
                第 {{ pagination.current }} / {{ pagination.pages }} 页
              </span>
              <button 
                class="btn btn-sm btn-secondary" 
                :disabled="pagination.current >= pagination.pages || loading"
                @click="() => handlePageChange(pagination.current + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Item 详情弹窗 -->
      <div v-if="showDetailModal" class="modal-overlay" @click.self="closeDetailModal">
        <div class="modal-content">
          <div class="modal-header">
            <h2>Item 详情设置</h2>
            <button class="modal-close-btn" @click="closeDetailModal">×</button>
          </div>
          <div class="modal-body" v-if="selectedMapping && itemDetail">
            <div class="detail-subtitle" style="margin-bottom: 16px; color: #666; font-size: 13px;">
              Item ID: {{ selectedMapping.itemId }}, 模型: {{ selectedMapping.modelName }}, Key: {{ selectedMapping.key }}, Post Param: {{ selectedMapping.postParam }}
            </div>
            
            <!-- 基本信息 -->
            <div class="detail-section">
              <h3>基本信息</h3>
              <div class="detail-grid">
                <div class="detail-item">
                  <label>ID:</label>
                  <span>{{ itemDetail.id || '-' }}</span>
                </div>
                <div class="detail-item">
                  <label>Model Name:</label>
                  <span>{{ itemDetail.modelName || '-' }}</span>
                </div>
                <div class="detail-item">
                  <label>Item Type:</label>
                  <span>{{ itemDetail.itemType || '-' }}</span>
                </div>
                <div class="detail-item">
                  <label>Sort Order:</label>
                  <span>{{ itemDetail.sortOrder != null ? itemDetail.sortOrder : '-' }}</span>
                </div>
              </div>
            </div>

            <!-- 使用 ConfigItemEditor 展示配置项详情（支持嵌套展示） -->
            <div class="detail-section">
              <h3>配置项详情</h3>
              <div v-if="configItem">
                <ConfigItemEditor
                  :key="`config-item-${itemDetail?.id || 'new'}-${refreshKey}`"
                  :item="configItem"
                  :key-placeholder="'例如: prompt'"
                  :node-placeholder="'例如: content[0].text'"
                  :post-param-placeholder="'例如: text'"
                  :default-value-placeholder="'例如: text'"
                  :request-body="{}"
                  :nested="false"
                  @update:item="handleItemUpdate"
                />
              </div>
              <div v-else style="color: #999; padding: 20px; text-align: center;">
                加载中...
              </div>
            </div>
          </div>
          <div class="modal-body" v-else style="padding: 40px; text-align: center; color: #999;">
            加载中...
          </div>
          <div class="modal-footer">
            <button class="btn btn-primary" @click="handleSaveItem" :disabled="saving">
              {{ saving ? '保存中...' : '💾 保存' }}
            </button>
            <button class="btn btn-secondary" @click="closeDetailModal">关闭</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { searchDictionary, getItemDetail, updateItemDetail, getDictionaryOptions } from '../../api/dictionary'
import ConfigItemEditor from '../reverse/ConfigItemEditor.vue'

const searchForm = reactive({
  modelName: '',
  key: '',
  postParam: ''
})

const mappingList = ref([])
const selectedMapping = ref(null)
const itemDetail = ref(null)
const loading = ref(false)
const error = ref('')
const showDetailModal = ref(false)
const saving = ref(false)
const editedConfigItem = ref(null) // 保存编辑后的配置项
const refreshKey = ref(0) // 用于强制刷新组件

// 下拉选项数据
const options = reactive({
  modelNames: [],
  keys: [],
  postParams: []
})

// 分页信息
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
  pages: 0
})

// 查询映射关系
const handleSearch = async (pageNum = 1) => {
  loading.value = true
  error.value = ''
  mappingList.value = []
  selectedMapping.value = null
  itemDetail.value = null
  pagination.current = pageNum

  try {
    const params = {
      pageSize: pagination.size,
      pageNumber: pagination.current
    }
    if (searchForm.modelName && searchForm.modelName.trim()) {
      params.modelName = searchForm.modelName.trim()
    }
    if (searchForm.key && searchForm.key.trim()) {
      params.key = searchForm.key.trim()
    }
    if (searchForm.postParam && searchForm.postParam.trim()) {
      params.postParam = searchForm.postParam.trim()
    }

    const response = await searchDictionary(params)

    if (response.code === 200) {
      // response.data 是 DictionarySearchResponse 对象，包含 mappingList 和分页信息
      mappingList.value = response.data?.mappingList || []
      pagination.total = response.data?.total || 0
      pagination.pages = response.data?.pages || 0
      pagination.current = response.data?.current || 1
      pagination.size = response.data?.size || 10
      
      if (mappingList.value.length === 0) {
        error.value = '未找到匹配的映射关系'
      } else {
        error.value = '' // 清除之前的错误
      }
    } else {
      error.value = response.message || '查询失败'
      mappingList.value = []
      pagination.total = 0
      pagination.pages = 0
    }
  } catch (err) {
    error.value = err.message || '请求失败，请检查网络连接'
    console.error('Search error:', err)
    mappingList.value = []
    pagination.total = 0
    pagination.pages = 0
  } finally {
    loading.value = false
  }
}

// 分页切换
const handlePageChange = (pageNum) => {
  // 确保 pageNum 是数字类型
  const page = typeof pageNum === 'number' ? pageNum : parseInt(pageNum)
  if (page >= 1 && page <= pagination.pages) {
    handleSearch(page)
  }
}

// 重置查询条件
const handleReset = () => {
  searchForm.modelName = ''
  searchForm.key = ''
  searchForm.postParam = ''
  mappingList.value = []
  selectedMapping.value = null
  itemDetail.value = null
  error.value = ''
  showDetailModal.value = false
  pagination.current = 1
  pagination.total = 0
  pagination.pages = 0
}

// 选择映射关系
const selectMapping = async (mapping) => {
  selectedMapping.value = mapping
  await viewItemDetail(mapping)
}

// 查看 Item 详情
const viewItemDetail = async (mapping) => {
  if (!mapping.itemId) {
    error.value = '缺少必要参数：itemId'
    itemDetail.value = null
    selectedMapping.value = null
    showDetailModal.value = false
    return
  }

  // 先清除之前的编辑状态和详情数据，确保显示最新数据
  editedConfigItem.value = null
  itemDetail.value = null
  // 先设置 selectedMapping，然后打开弹窗
  selectedMapping.value = mapping
  showDetailModal.value = true

  try {
    const response = await getItemDetail(mapping.itemId)
    if (response.code === 200) {
      // 更新详情数据
      itemDetail.value = response.data
      // 确保清除编辑状态，让 configItem 使用最新的 itemDetail
      editedConfigItem.value = null
      // 等待下一个 tick 确保数据已更新
      await nextTick()
      // 增加 refreshKey 强制刷新组件
      refreshKey.value++
      error.value = '' // 清除之前的错误
      console.log('Item detail loaded:', itemDetail.value)
      console.log('Config item computed:', configItem.value)
    } else {
      error.value = response.message || '获取详情失败'
      itemDetail.value = null
    }
  } catch (err) {
    error.value = err.message || '获取详情失败'
    console.error('Get item detail error:', err)
    itemDetail.value = null
  }
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  selectedMapping.value = null
  itemDetail.value = null
  editedConfigItem.value = null
}

// 将 itemDetail 转换为 ConfigItemEditor 需要的格式
const configItem = computed(() => {
  // 如果有编辑中的配置项，优先使用编辑中的数据（用于实时编辑）
  // 否则使用 itemDetail 的数据（用于显示保存后的最新数据）
  const source = editedConfigItem.value || itemDetail.value
  
  if (!source) {
    return {
      key: '',
      category: 'key',
      node: '',
      postParam: '',
      valueObject: 'string',
      defaultValue: '',
      spelTemp: '',
      validate: ''
    }
  }
  
  const item = {
    key: source.itemKey || source.key || '',
    category: source.category || 'key',
    node: source.node || '',
    postParam: source.postParam || '',
    valueObject: source.valueObject || 'string',
    defaultValue: source.defaultValue || '',
    spelTemp: source.spelTemp || '',
    validate: source.validate || ''
  }
  
  return item
})

// 处理配置项更新
const handleItemUpdate = (updatedItem) => {
  // 保存编辑后的配置项
  editedConfigItem.value = updatedItem
  console.log('配置项更新:', updatedItem)
}

// 保存 Item 详情
const handleSaveItem = async () => {
  if (!selectedMapping.value || !selectedMapping.value.itemId) {
    error.value = '缺少必要参数：itemId'
    return
  }

  if (!editedConfigItem.value) {
    error.value = '没有修改内容'
    return
  }

  saving.value = true
  error.value = ''

  try {
    // 将编辑后的配置项转换为后端需要的格式
    // 注意：spelTemp 和 validate 是 JSON 类型，空字符串需要转换为 null
    const spelTemp = editedConfigItem.value.spelTemp || itemDetail.value.spelTemp || ''
    const validate = editedConfigItem.value.validate || itemDetail.value.validate || ''
    
    const updateData = {
      itemKey: editedConfigItem.value.key || itemDetail.value.itemKey,
      category: editedConfigItem.value.category || itemDetail.value.category,
      node: editedConfigItem.value.node || itemDetail.value.node,
      postParam: editedConfigItem.value.postParam || itemDetail.value.postParam,
      valueObject: editedConfigItem.value.valueObject || itemDetail.value.valueObject,
      defaultValue: editedConfigItem.value.defaultValue || itemDetail.value.defaultValue,
      spelTemp: spelTemp.trim() === '' ? null : spelTemp,
      validate: validate.trim() === '' ? null : validate,
      sortOrder: itemDetail.value.sortOrder
    }

    const response = await updateItemDetail(selectedMapping.value.itemId, updateData)

    if (response.code === 200) {
      // 保存当前选中的映射关系，避免被 handleSearch 清除
      const currentMapping = { ...selectedMapping.value }
      
      // 刷新列表数据（重新查询当前页）
      await handleSearch(pagination.current)
      
      // 恢复选中的映射关系，确保弹窗继续显示
      selectedMapping.value = currentMapping
      
      // 刷新详情数据（重新获取最新数据）
      try {
        const detailResponse = await getItemDetail(selectedMapping.value.itemId)
        if (detailResponse.code === 200) {
          // 先更新数据
          itemDetail.value = detailResponse.data
          // 清除编辑状态，让 configItem 使用最新的 itemDetail
          editedConfigItem.value = null
          // 强制刷新组件（通过改变 key）
          refreshKey.value++
          // 等待下一个 tick 确保组件已更新
          await nextTick()
          error.value = '' // 清除错误
          console.log('保存成功，已刷新详情数据:', itemDetail.value)
        }
      } catch (err) {
        console.error('Refresh item detail error:', err)
        error.value = '保存成功，但刷新详情失败: ' + err.message
      }
      
      alert('保存成功！')
    } else {
      error.value = response.message || '保存失败'
    }
  } catch (err) {
    error.value = err.message || '保存失败，请检查网络连接'
    console.error('Save item error:', err)
  } finally {
    saving.value = false
  }
}

// 加载下拉选项数据
const loadOptions = async () => {
  try {
    const response = await getDictionaryOptions()
    if (response.code === 200 && response.data) {
      options.modelNames = response.data.modelNames || []
      options.keys = response.data.keys || []
      options.postParams = response.data.postParams || []
      console.log('字典选项加载成功:', options)
    }
  } catch (err) {
    console.error('加载字典选项失败:', err)
    // 不显示错误，因为选项加载失败不影响查询功能
  }
}

// 页面加载时获取选项数据
onMounted(() => {
  loadOptions()
})
</script>

<style scoped>
.dictionary-query-page {
  padding: 20px;
}

.search-form {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-row label {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.form-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-input:focus {
  outline: none;
  border-color: #3498db;
}

.form-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-start;
  margin-top: 8px;
}

.mapping-table {
  overflow-x: auto;
}

.mapping-table table {
  width: 100%;
  border-collapse: collapse;
}

.mapping-table th,
.mapping-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.mapping-table th {
  background: #f5f5f5;
  font-weight: 600;
  color: #333;
  position: sticky;
  top: 0;
}

.mapping-table tbody tr {
  cursor: pointer;
  transition: background 0.2s;
}

.mapping-table tbody tr:hover {
  background: #f9f9f9;
}

.mapping-table tbody tr.selected {
  background: #e3f2fd;
}

.count-badge {
  font-size: 12px;
  font-weight: normal;
  color: #666;
  margin-left: 8px;
}

.detail-subtitle {
  font-size: 12px;
  font-weight: normal;
  color: #666;
  margin-left: 12px;
}

.item-detail {
  margin-top: 16px;
}

.detail-section {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.detail-section:last-child {
  border-bottom: none;
}

.detail-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.detail-item span {
  font-size: 14px;
  color: #333;
  word-break: break-all;
}

.code-block {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  overflow-x: auto;
  max-height: 400px;
  overflow-y: auto;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.modal-close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.modal-close-btn:hover {
  background: #f5f5f5;
  color: #333;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #e0e0e0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 分页样式 */
.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  margin-top: 16px;
  border-top: 1px solid #e0e0e0;
}

.pagination-info {
  font-size: 13px;
  color: #666;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-info {
  font-size: 13px;
  color: #333;
  padding: 0 8px;
}

.pagination-controls .btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>

