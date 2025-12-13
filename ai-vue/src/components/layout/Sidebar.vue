<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <h2>AI Model Tool</h2>
    </div>
    <div class="sidebar-menu">
      <div 
        class="menu-item" 
        :class="{ active: currentPage === 'reverse' }"
        @click="switchPage('reverse')"
      >
        <span class="menu-icon">🔄</span>
        <span class="menu-text">反向解析</span>
      </div>
      <div 
        class="menu-item" 
        :class="{ active: currentPage === 'forward' }"
        @click="switchPage('forward')"
      >
        <span class="menu-icon">➡️</span>
        <span class="menu-text">正向请求</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  currentPage: {
    type: String,
    default: 'reverse'
  }
})

const emit = defineEmits(['switch-page'])

const switchPage = (page) => {
  emit('switch-page', page)
}
</script>

<style scoped>
.sidebar {
  width: 200px;
  background: #2c3e50;
  color: white;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #34495e;
}

.sidebar-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.sidebar-menu {
  flex: 1;
  padding: 20px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  cursor: pointer;
  transition: all 0.3s;
  border-left: 3px solid transparent;
}

.menu-item:hover {
  background: #34495e;
}

.menu-item.active {
  background: #34495e;
  border-left-color: #3498db;
}

.menu-icon {
  font-size: 20px;
  margin-right: 12px;
}

.menu-text {
  font-size: 14px;
}
</style>

