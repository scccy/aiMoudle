/**
 * 配置项工具函数
 * 用于转换配置项格式
 */

/**
 * 将配置项转换为保存格式
 * @param {Array} items - 配置项数组
 * @param {boolean} parseValidate - 是否解析 validate 为对象（false 则保持字符串）
 * @returns {Array} 转换后的配置项数组
 */
export const convertItemsToSaveFormat = (items, parseValidate = false) => {
  return items.map(item => {
    const result = {
      key: item.key,
      category: item.category,
      node: item.node,
      postParam: item.postParam,
      valueObject: item.valueObject
    }
    
    if (item.defaultValue && item.defaultValue.trim()) {
      result.defaultValue = item.defaultValue.trim()
    }
    
    if (item.spelTemp && item.spelTemp.trim()) {
      result.spelTemp = item.spelTemp.trim()
    }
    
    if (item.validate && item.validate.trim()) {
      if (parseValidate) {
        try {
          result.validate = JSON.parse(item.validate.trim())
        } catch (e) {
          // 如果解析失败，保持为字符串
          result.validate = item.validate.trim()
        }
      } else {
        // 保持为字符串格式
        result.validate = item.validate.trim()
      }
    }
    
    return result
  })
}

