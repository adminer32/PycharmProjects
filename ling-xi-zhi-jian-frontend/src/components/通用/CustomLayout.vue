<script setup lang="ts">
import { computed } from 'vue'

interface Item {
  title: string
  content: string
  image?: string
}

const props = defineProps<{
  mainTitle: string
  mainContent: string
  items: Item[]
}>()

const defaultImages = [
  'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400&h=300&fit=crop',
  'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=300&fit=crop',
  'https://images.unsplash.com/photo-1434682881908-b43d0467b798?w=400&h=300&fit=crop',
  'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=400&h=300&fit=crop'
]

const itemsWithImages = computed(() => {
  return props.items.map((item, index) => ({
    ...item,
    image: item.image || defaultImages[index % defaultImages.length]
  }))
})
</script>

<template>
  <div class="custom-layout">
    <div class="layout-header">
      <h2 class="main-title">{{ mainTitle }}</h2>
      <p class="main-content">{{ mainContent }}</p>
    </div>
    
    <div class="cards-container">
      <div 
        v-for="(item, index) in itemsWithImages" 
        :key="index"
        class="feature-card"
        :style="{ '--delay': `${index * 0.1}s` }"
      >
        <div class="card-image-wrapper">
          <img :src="item.image" :alt="item.title" class="card-image" />
          <div class="card-overlay"></div>
        </div>
        <div class="card-content">
          <div class="card-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path v-if="index === 0" d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
              <path v-else-if="index === 1" d="M14.5 10c-.83 0-1.5-.67-1.5-1.5v-5c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5v5c0 .83-.67 1.5-1.5 1.5zM20.5 10H19V8.5c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM9.5 14c0 .83-.67 1.5-1.5 1.5S6.5 14.83 6.5 14v-3c0-.83.67-1.5 1.5-1.5S9.5 10.17 9.5 11v3z"/>
              <path v-else-if="index === 2" d="M22 12h-4l-3 9L9 3l-3 9H2"/>
              <path v-else d="M12 2a10 10 0 1 0 10 10H12V2z"/>
            </svg>
          </div>
          <h3 class="card-title">{{ item.title }}</h3>
          <p class="card-text">{{ item.content }}</p>
        </div>
        <div class="card-decoration">
          <div class="decoration-line"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.custom-layout {
  padding: 40px 60px 60px;
  background: linear-gradient(180deg, rgba(255,255,255,0) 0%, rgba(240,247,255,0.8) 100%);

  .layout-header {
    text-align: center;
    margin-bottom: 50px;

    .main-title {
      font-size: 2.2rem;
      font-weight: 700;
      background: linear-gradient(135deg, #69c0ff 0%, #1890ff 50%, #69c0ff 100%);
      background-size: 200% auto;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      margin-bottom: 16px;
      animation: gradient-shift 3s ease infinite;
    }

    .main-content {
      font-size: 1.1rem;
      color: #64748b;
      max-width: 700px;
      margin: 0 auto;
      line-height: 1.8;
    }
  }

  .cards-container {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 24px;
    max-width: 1400px;
    margin: 0 auto;
  }

  .feature-card {
    position: relative;
    border-radius: 20px;
    overflow: hidden;
    background: #fff;
    box-shadow: 
      0 4px 6px -1px rgba(0, 0, 0, 0.05),
      0 10px 15px -3px rgba(0, 0, 0, 0.08);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    animation: card-appear 0.6s ease backwards;
    animation-delay: var(--delay);

    &:hover {
      transform: translateY(-8px) scale(1.02);
      box-shadow: 
        0 20px 40px -10px rgba(105, 192, 255, 0.35),
        0 10px 20px -5px rgba(135, 206, 250, 0.25);

      .card-image {
        transform: scale(1.1);
      }

      .card-overlay {
        opacity: 0.7;
      }

      .card-title {
        color: #69c0ff;
      }

      .decoration-line {
        width: 100%;
        background: linear-gradient(90deg, #69c0ff, #1890ff);
      }
    }

    .card-image-wrapper {
      position: relative;
      height: 140px;
      overflow: hidden;

      .card-image {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.5s ease;
      }

      .card-overlay {
        position: absolute;
        inset: 0;
        background: linear-gradient(180deg, transparent 0%, rgba(176, 224, 250, 0.5) 100%);
        opacity: 0.5;
        transition: opacity 0.3s ease;
      }
    }

    .card-content {
      padding: 20px;
      position: relative;
      z-index: 1;

      .card-icon {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        background: linear-gradient(135deg, rgba(105,192,255,0.15) 0%, rgba(176,224,250,0.15) 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 14px;

        svg {
          width: 22px;
          height: 22px;
          color: #69c0ff;
        }
      }

      .card-title {
        font-size: 1.15rem;
        font-weight: 600;
        color: #1e293b;
        margin-bottom: 8px;
        transition: color 0.3s ease;
      }

      .card-text {
        font-size: 0.9rem;
        color: #64748b;
        line-height: 1.6;
        margin: 0;
      }
    }

    .card-decoration {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 3px;
      padding: 0 20px;

      .decoration-line {
        height: 100%;
        width: 40px;
        background: linear-gradient(90deg, #69c0ff, #1890ff);
        border-radius: 2px;
        transition: all 0.4s ease;
      }
    }
  }
}

@keyframes gradient-shift {
  0%, 100% { background-position: 0% center; }
  50% { background-position: 200% center; }
}

@keyframes card-appear {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1200px) {
  .custom-layout .cards-container {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .custom-layout {
    padding: 30px 20px;

    .cards-container {
      grid-template-columns: 1fr;
    }
  }
}
</style>
