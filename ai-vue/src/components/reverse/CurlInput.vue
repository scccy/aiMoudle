<template>
  <div class="card">
    <div class="card-title">CURL 命令输入</div>
    <div class="input-group">
      <label>粘贴 CURL 命令，自动解析模型配置信息</label>
      <textarea
        v-model="curlText"
        :placeholder="curlPlaceholder"
        @input="handleCurlChange"
        class="json-textarea"
        style="min-height: 150px;"
      ></textarea>
    </div>
    <button class="btn btn-primary" style="width: 100%;" @click="parseCurl">
      解析并填充
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['parseResult'])

const curlText = ref('')
const curlPlaceholder = `curl -X POST https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer $ARK_API_KEY" \\
  -d '{...}'
`

const parseCurl = () => {
  try {
    const result = parseCurlCommand(curlText.value)
    emit('parseResult', result)
  } catch (error) {
    alert('解析失败: ' + error.message)
  }
}

const handleCurlChange = () => {
  // 可以实时解析，或者只在点击按钮时解析
}

/**
 * 解析 CURL 命令
 */
function parseCurlCommand(curlStr) {
  const result = {
    baseUrl: '',
    point: '',
    authorization: '',
    requestHeaders: {},
    requestBody: {}
  }

  if (!curlStr || !curlStr.trim()) {
    throw new Error('请输入 CURL 命令')
  }

  // 提取 URL - 支持多种格式
  const urlPatterns = [
    /curl\s+.*?\s+(https?:\/\/[^\s"']+)/i,
    /(https?:\/\/[^\s"']+)/i
  ]
  
  let fullUrl = null
  for (const pattern of urlPatterns) {
    const match = curlStr.match(pattern)
    if (match && match[1]) {
      fullUrl = match[1]
      break
    }
  }
  
  if (fullUrl) {
    try {
      const url = new URL(fullUrl)
      const pathname = url.pathname
      
      // 尝试智能分割路径
      // 如果路径包含 /api/v3 或类似的前缀，将其作为 base_url 的一部分
      // 例如: /api/v3/contents/generations/tasks
      // base_url: https://host/api/v3
      // point: /contents/generations/tasks
      
      // 匹配常见的 API 路径模式: /api/v1, /api/v2, /api/v3 等
      const apiPrefixMatch = pathname.match(/^(\/api\/v\d+)/)
      if (apiPrefixMatch) {
        result.baseUrl = `${url.protocol}//${url.host}${apiPrefixMatch[1]}`
        result.point = pathname.substring(apiPrefixMatch[1].length) || '/'
      } else {
        // 如果没有匹配到，使用 host 作为 base_url，整个 pathname 作为 point
        result.baseUrl = `${url.protocol}//${url.host}`
        result.point = pathname || '/'
      }
    } catch (e) {
      // 如果 URL 解析失败，尝试简单分割
      const parts = fullUrl.split('/')
      if (parts.length >= 3) {
        result.baseUrl = `${parts[0]}//${parts[2]}`
        result.point = '/' + parts.slice(3).join('/')
      }
    }
  }

  // 提取所有 Headers (支持 -H 和 --header 两种格式)
  const headerPattern = /(?:-H|--header)\s+["']([^"']+):\s*([^"']+)["']/gi
  const headers = {}
  let headerMatch
  
  while ((headerMatch = headerPattern.exec(curlStr)) !== null) {
    const headerName = headerMatch[1].trim()
    const headerValue = headerMatch[2].trim()
    headers[headerName] = headerValue
    
    // 单独提取 Authorization
    if (headerName.toLowerCase() === 'authorization') {
      result.authorization = headerValue
    }
  }
  
  // 如果有提取到headers，添加到结果中
  if (Object.keys(headers).length > 0) {
    result.requestHeaders = headers
  }

  // 提取 -d 或 --data-raw 后面的 JSON body
  // 支持多种格式：-d '{...}' 或 --data-raw '{...}' 或多行格式
  let jsonStr = null
  // 同时匹配 -d 和 --data-raw
  const dataPattern = /(?:-d|--data-raw)\s+/i
  const dataIndex = curlStr.search(dataPattern)
  if (dataIndex !== -1) {
    // 找到 -d 或 --data-raw 后面的内容开始位置
    const match = curlStr.substring(dataIndex).match(dataPattern)
    let startPos = dataIndex + match[0].length
    let remaining = curlStr.substring(startPos).trim()
    
    // 检查第一个字符是否是引号
    if (remaining.length > 0) {
      const firstChar = remaining[0]
      if (firstChar === "'" || firstChar === '"' || firstChar === '`') {
        // 找到匹配的结束引号（简单匹配，不考虑转义）
        // 从第二个字符开始查找
        let endPos = -1
        for (let i = 1; i < remaining.length; i++) {
          if (remaining[i] === firstChar && remaining[i - 1] !== '\\') {
            endPos = i
            break
          }
        }
        
        if (endPos !== -1) {
          // 提取引号内的内容
          jsonStr = remaining.substring(1, endPos)
        } else {
          // 没找到结束引号，尝试提取到字符串末尾
          jsonStr = remaining.substring(1)
        }
      } else {
        // 没有引号，尝试直接提取 JSON 对象
        const jsonMatch = remaining.match(/\{[\s\S]*\}/)
        if (jsonMatch) {
          jsonStr = jsonMatch[0]
        } else {
          jsonStr = remaining
        }
      }
    }
  } else {
    // 如果没找到 -d，尝试直接找 JSON 对象
    const jsonMatch = curlStr.match(/\{[\s\S]*\}/)
    if (jsonMatch) {
      jsonStr = jsonMatch[0]
    }
  }
  
  if (jsonStr) {
    try {
      // 清理转义字符
      jsonStr = jsonStr
        .replace(/\\'/g, "'")
        .replace(/\\"/g, '"')
        .replace(/\\n/g, '\n')
        .replace(/\\t/g, '\t')
        .replace(/\\r/g, '\r')
      result.requestBody = JSON.parse(jsonStr)
    } catch (e) {
      // 如果解析失败，尝试直接解析原始字符串
      try {
        result.requestBody = JSON.parse(jsonStr.trim())
      } catch (e2) {
        console.error('JSON 解析错误:', e2, '原始字符串:', jsonStr)
        throw new Error('无法解析 JSON body: ' + e2.message)
      }
    }
  }

  return result
}
</script>

