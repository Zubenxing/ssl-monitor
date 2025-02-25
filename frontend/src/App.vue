<script setup>
import DomainList from './components/DomainList.vue'
</script>

<template>
  <router-view></router-view>
</template>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
  overflow-x: hidden;
}

#app {
  height: 100%;
  width: 100%;
  position: absolute;
  top: 0;
  left: 0;
}
</style>
