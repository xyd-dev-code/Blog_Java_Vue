<template>
  <span>{{ display }}</span>
</template>

<script setup>
import { watch } from 'vue'
import { useCountUp } from '@/composables/useCountUp'

const props = defineProps({
  value: { type: [Number, String], default: 0 },
  duration: { type: Number, default: 1400 },
  decimals: { type: Number, default: 0 }
})

const { display, restart } = useCountUp(props.value, {
  duration: props.duration,
  decimals: props.decimals
})

watch(() => props.value, (newVal) => {
  restart(newVal)
})
</script>
