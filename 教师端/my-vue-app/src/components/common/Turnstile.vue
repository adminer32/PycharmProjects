<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

const props = defineProps<{
  siteKey: string;
  action?: string;
}>();

const emit = defineEmits<{
  (e: 'verify', callbackToken: string): void;
  (e: 'error', error: any): void;
}>();

const turnstileContainer = ref<HTMLElement | null>(null);
let widgetId: any = null;
onMounted(() => {
  if (!window.turnstile) {
    const script = document.createElement('script');
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
    script.async = true;
    script.defer = true;
    script.onload = renderTurnstile;
    document.head.appendChild(script);
  } else {
    renderTurnstile();
  }
});

onUnmounted(() => {
  if (widgetId !== null && window.turnstile) {
    window.turnstile.remove(widgetId);
  }
});

const renderTurnstile = () => {
  if (!turnstileContainer.value || !window.turnstile) return;

  widgetId = window.turnstile.render(turnstileContainer.value, {
    sitekey: props.siteKey,
    action: props.action || 'login',
    callback: (token: string) => emit('verify', token),
    'error-callback': (err: any) => emit('error', err),
    size: 'invisible'
  });
};

const execute = () => {
  if (widgetId !== null && window.turnstile) {
    window.turnstile.execute(container, {
      action: props.action
    });
  }
}

defineExpose({
  execute,
  reset: () => {
    if (widgetId !== null && window.turnstile) {
      window.turnstile.reset(widgetId);
    }
  }
});
</script>

<template>
  <!-- Invisible container -->
  <div ref="turnstileContainer" style="display: none;"></div>
</template>
