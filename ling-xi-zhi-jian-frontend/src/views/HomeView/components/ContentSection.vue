<script lang="ts" setup>
import { ref } from 'vue';
import ShuttlecockSport from './ContentSection/ShuttlecockSport.vue';
import ShuttlecockIntroduction from './ContentSection/ShuttlecockIntroduction.vue';
import ShuttlecockHistory from './ContentSection/ShuttlecockHistory.vue';
import ShuttlecockClassification from './ContentSection/ShuttlecockClassification.vue';
import ShuttlecockDevelopment from './ContentSection/ShuttlecockDevelopment.vue';

import img1 from '@/assets/content_section/踢毽子.png';
import img2 from '@/assets/content_section/毽球的介绍.png';
import img3 from '@/assets/content_section/毽球的起源.png';
import img4 from '@/assets/content_section/毽球的分类.png';
import img5 from '@/assets/content_section/毽球的发展.png';

// 定义组件数据
const components = ref([
  { id: 1, title: '踢毽子（体育运动）', component: ShuttlecockSport, image: img1, description: '毽球是一种隔网相争的体育项目，也是中国汉族传统民间体育活动' },
  { id: 2, title: '毽球介绍', component: ShuttlecockIntroduction, image: img2, description: '毽球运动是在我国民间踢毽活动基础上发展起来的民族传统体育项目' },
  { id: 3, title: '毽球的历史起源', component: ShuttlecockHistory, image: img3, description: '踢毽子起源于中国汉代，唐宋时期开始盛行，清代达到鼎盛时期' },
  { id: 4, title: '毽球的分类', component: ShuttlecockClassification, image: img4, description: '毽球运动分为花毽和网毽（毽球）两大类' },
  { id: 5, title: '毽球的发展', component: ShuttlecockDevelopment, image: img5, description: '新中国成立后，毽球运动得到大力扶植和发展，成为全国普遍开展的热门项目' }
]);

// 左侧大盒子的索引
const leftIndex = ref(0);

// 右侧盒子的索引数组
const rightIndexes = ref([1, 2, 3, 4]);

// 点击右侧盒子时交换内容
const handleItemClick = (index: number) => {
  const rightIndex = rightIndexes.value[index];
  const temp = leftIndex.value;
  leftIndex.value = rightIndex;
  rightIndexes.value[index] = temp;
};
</script>

<template>
  <div class="content-section">
    <div class="left-section">
      <div class="main-card" @click="handleItemClick(0)">
        <div class="card-bg">
          <img :src="components[leftIndex].image" alt="主图" />
          <div class="bg-overlay"></div>
        </div>
        <div class="card-panel">
          <div class="panel-header">
            <span class="panel-tag">翎翼毽球</span>
            <h2>{{ components[leftIndex].title }}</h2>
          </div>
          <div class="panel-content">
            <component :is="components[leftIndex].component" />
          </div>
          <div class="panel-footer">
            <span class="learn-more">
              点击了解更多
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </span>
          </div>
        </div>
      </div>
    </div>
    <div class="right-section">
      <div 
        class="item-card" 
        v-for="(index, i) in rightIndexes" 
        :key="i" 
        @click="handleItemClick(i)"
      >
        <div class="card-bg">
          <img :src="components[index].image" alt="图片" />
          <div class="bg-overlay"></div>
        </div>
        <div class="card-panel">
          <span class="panel-tag">翎翼毽球</span>
          <h3>{{ components[index].title }}</h3>
          <p>{{ components[index].description }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.content-section {
  display: flex;
  gap: 24px;
  padding: 40px 60px;
  width: 100%;
  height: 85vh;
  max-width: 1600px;
  margin: 0 auto;
  transform-origin: center;

  .left-section {
    flex: 2.2;
    display: flex;

    .main-card {
      flex: 1;
      position: relative;
      border-radius: 24px;
      overflow: hidden;
      box-shadow: 
        0 10px 40px -10px rgba(0, 0, 0, 0.15),
        0 4px 10px -2px rgba(0, 0, 0, 0.08);
      cursor: pointer;
      transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover {
        transform: translateY(-4px);
        box-shadow: 
          0 20px 50px -12px rgba(105, 192, 255, 0.3),
          0 8px 16px -4px rgba(135, 206, 250, 0.2);

        .card-bg img {
          transform: scale(1.08);
        }

        .panel-footer .learn-more svg {
          transform: translateX(4px);
        }
      }

      .card-bg {
        position: absolute;
        inset: 0;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.6s ease;
        }

        .bg-overlay {
          position: absolute;
          inset: 0;
          background: linear-gradient(
            135deg,
            rgba(105, 192, 255, 0.75) 0%,
            rgba(135, 206, 250, 0.6) 50%,
            rgba(176, 224, 250, 0.5) 100%
          );
        }
      }

      .card-panel {
        position: relative;
        z-index: 1;
        height: 100%;
        display: flex;
        flex-direction: column;
        padding: 36px;
        color: #fff;

        .panel-header {
          margin-bottom: auto;

          .panel-tag {
            display: inline-block;
            padding: 6px 14px;
            background: rgba(255, 255, 255, 0.2);
            backdrop-filter: blur(10px);
            border-radius: 20px;
            font-size: 0.8rem;
            font-weight: 500;
            margin-bottom: 16px;
          }

          h2 {
            margin: 0;
            font-size: 2rem;
            font-weight: 700;
            line-height: 1.3;
            text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
          }
        }

        .panel-content {
          flex: 1;
          overflow-y: auto;
          margin: 20px 0;
          padding-right: 10px;

          &::-webkit-scrollbar {
            width: 4px;
          }

          &::-webkit-scrollbar-track {
            background: rgba(255,255,255,0.1);
            border-radius: 2px;
          }

          &::-webkit-scrollbar-thumb {
            background: rgba(255,255,255,0.3);
            border-radius: 2px;
          }

          :deep(.shuttlecock-sport),
          :deep(.shuttlecock-introduction),
          :deep(.shuttlecock-history),
          :deep(.shuttlecock-classification),
          :deep(.shuttlecock-development) {
            padding: 0;

            h2 {
              display: none;
            }

            p,
            ul {
              color: rgba(255, 255, 255, 0.95);
              font-size: 1rem;
              line-height: 1.8;
            }

            ul {
              padding-left: 20px;
              
              li {
                margin-bottom: 10px;
              }
            }
          }
        }

        .panel-footer {
          margin-top: auto;
          padding-top: 20px;
          border-top: 1px solid rgba(255, 255, 255, 0.2);

          .learn-more {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            font-size: 0.95rem;
            font-weight: 500;
            opacity: 0.9;

            svg {
              width: 18px;
              height: 18px;
              transition: transform 0.3s ease;
            }
          }
        }
      }
    }
  }

  .right-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16px;

    .item-card {
      flex: 1;
      position: relative;
      border-radius: 20px;
      overflow: hidden;
      box-shadow: 
        0 4px 12px -2px rgba(0, 0, 0, 0.08);
      cursor: pointer;
      transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover {
        transform: translateY(-3px) scale(1.02);
        box-shadow: 
          0 16px 32px -8px rgba(105, 192, 255, 0.25),
          0 6px 12px -3px rgba(135, 206, 250, 0.15);

        .card-bg img {
          transform: scale(1.1);
        }

        .panel-footer .learn-more svg {
          transform: translateX(4px);
        }
      }

      .card-bg {
        position: absolute;
        inset: 0;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.5s ease;
        }

        .bg-overlay {
          position: absolute;
          inset: 0;
          background: linear-gradient(
            135deg,
            rgba(105, 192, 255, 0.78) 0%,
            rgba(135, 206, 250, 0.65) 100%
          );
        }
      }

      .card-panel {
        position: relative;
        z-index: 1;
        height: 100%;
        display: flex;
        flex-direction: column;
        padding: 24px;
        color: #fff;

        .panel-tag {
          display: inline-block;
          width: fit-content;
          padding: 4px 12px;
          background: rgba(255, 255, 255, 0.2);
          backdrop-filter: blur(8px);
          border-radius: 12px;
          font-size: 0.75rem;
          font-weight: 500;
          margin-bottom: 12px;
        }

        h3 {
          margin: 0 0 10px 0;
          font-size: 1.25rem;
          font-weight: 600;
          line-height: 1.3;
        }

        p {
          margin: 0;
          font-size: 0.85rem;
          line-height: 1.5;
          opacity: 0.9;
          flex: 1;
        }

        .panel-footer {
          margin-top: auto;
          padding-top: 12px;

          .learn-more {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            font-size: 0.85rem;
            font-weight: 500;
            opacity: 0.85;

            svg {
              width: 16px;
              height: 16px;
              transition: transform 0.3s ease;
            }
          }
        }
      }
    }
  }
}

@media (max-width: 1200px) {
  .content-section {
    flex-direction: column;
    height: auto;
    padding: 20px 30px;

    .left-section {
      min-height: 450px;
    }

    .right-section {
      flex-direction: row;
      flex-wrap: wrap;

      .item-card {
        flex: 1 1 calc(50% - 8px);
        min-height: 200px;
      }
    }
  }
}

@media (max-width: 768px) {
  .content-section {
    padding: 16px 20px;

    .right-section .item-card {
      flex: 1 1 100%;
      min-height: 180px;
    }
  }
}
</style>