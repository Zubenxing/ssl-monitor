<template>
  <div class="settings">
    <h2>系统设置</h2>
    <el-form :model="settings" label-width="120px">
      <el-form-item label="检查间隔(小时)">
        <el-input-number v-model="settings.checkInterval" :min="1" :max="24"></el-input-number>
      </el-form-item>
      <el-form-item label="提醒阈值(天)">
        <el-input-number v-model="settings.notifyThreshold" :min="1" :max="90"></el-input-number>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="saveSettings">保存设置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'Settings',
  data() {
    return {
      settings: {
        checkInterval: 12,
        notifyThreshold: 30
      }
    }
  },
  methods: {
    saveSettings() {
      // TODO: 保存设置到后端
      this.$message.success('设置已保存')
    }
  }
}
</script>

<style scoped>
.settings {
  padding: 20px;
  max-width: 600px;
}
</style> 