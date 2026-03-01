<template>
  <div class="stat-card card-glass">
    <div class="label">{{ title }}</div>
    <div class="value">{{ value }}<span class="suffix">{{ suffix }}</span></div>
  </div>
</template>
<script setup>
const props = defineProps({ title: String, value: [String, Number], suffix: { type: String, default: '' } });
</script>
<style scoped>
.stat-card { padding: 14px; border-radius: var(--radius-lg); }
.label { color: #9fb2ce; font-size: 12px; }
.value { font-size: 26px; font-weight: 700; color: #fff; }
.suffix { margin-left: 6px; font-size: 14px; color: #9fb2ce; }
</style>
