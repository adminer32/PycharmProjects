<script setup lang="ts">
import {computed, nextTick, onUnmounted, ref, useTemplateRef} from 'vue';
import {useRouter} from 'vue-router';
import {
  AmbientLight,
  AnimationAction,
  AnimationMixer,
  Clock,
  Color,
  EquirectangularReflectionMapping,
  GridHelper,
  Scene,
  SkeletonHelper,
  TextureLoader,
} from 'three';
import {ThreeUtil} from '@/utils/ThreeUtil';
import {useI18n} from 'vue-i18n';
import SmartClassImage from '@/assets/course_center_view/SmartClass.svg';
import PersonalAssessmentImage from '@/assets/nav/PersonalAssessment.svg';

const centerOptionRef = useTemplateRef<HTMLElement>('center-option');
const router = useRouter();
const clock = new Clock();
const isShowCurriculumCenter = ref(false);
let isFirstShowCurriculumCenter = true;
const {t} = useI18n();
const showCurriculumCenter = () => {
  if (isShowCurriculumCenter.value) return;
  isShowCurriculumCenter.value = true;

  if (isFirstShowCurriculumCenter) {
    isFirstShowCurriculumCenter = false;
    nextTick(() => {
      const defaultCanvas = centerOptionRef.value?.querySelector(
          '.motion-demo-default-canvas',
      ) as HTMLCanvasElement;
      const afterCanvas = centerOptionRef.value?.querySelector(
          '.motion-demo-after-canvas',
      ) as HTMLCanvasElement;

      const defaultCamera = ThreeUtil.createCamera(defaultCanvas);
      const defaultRenderer =
          ThreeUtil.createWebGLRenderer(defaultCanvas);
      defaultRenderer.setSize(
          defaultCanvas.offsetWidth,
          defaultCanvas.offsetHeight,
      );
      defaultCamera.position.set(0, 200, 300);
      defaultCamera.lookAt(0, 0, 0);

      const afterCamera = ThreeUtil.createCamera(afterCanvas);
      const afterRenderer = ThreeUtil.createWebGLRenderer(afterCanvas);
      afterRenderer.setSize(
          afterCanvas.offsetWidth,
          afterCanvas.offsetHeight,
      );
      afterCamera.position.set(0, 200, 300);
      afterCamera.lookAt(0, 0, 0);

      (function animate() {
        if (defaultCamera) {
          const d = clock.getDelta();
          defaultMixer?.update(d);
          boneMixer?.update(d);

          defaultRenderer!.render(defaultScene, defaultCamera);
          afterRenderer!.render(boneScene, afterCamera);
          afterAction!.time = defaultAction!.time;
        }
        animationFrameId = requestAnimationFrame(animate);
      })();
    });
  }
};

// ———————————————————————————————————————— THREE ————————————————————————————————————————————————
ThreeUtil.loadGlb('/motion_demo_view/model/scene/羽毛球场.glb').then(
    (model) => {
      const floor = model.scenes[0];
      floor.castShadow = true;
      floor.scale.set(70, 70, 70);
      defaultScene.add(floor);
    },
);
const textureLoader = new TextureLoader();
textureLoader
    .loadAsync('/motion_demo_view/model/scene/EnvMapTexture.jpg')
    .then((texture) => {
      texture.mapping = EquirectangularReflectionMapping;
      defaultScene.environment = texture;
      defaultScene.background = texture;
    });
let defaultMixer: AnimationMixer;
let boneMixer: AnimationMixer;

let defaultAction: AnimationAction;
let afterAction: AnimationAction;

Promise.all([
  ThreeUtil.loadFBX('/motion_demo_view/model/motion/大腿触球.fbx'),
  ThreeUtil.loadFBX('/motion_demo_view/model/bone/大腿触球.fbx'),
]).then((res) => {
  const defaultModel = res[0];
  defaultModel.castShadow = true;
  defaultMixer = new AnimationMixer(defaultModel);
  defaultScene.add(defaultModel);

  const boneModel = res[1];
  boneMixer = new AnimationMixer(boneModel);
  boneScene.add(boneModel);

  const gridHelper = new GridHelper(
      10000,
      200,
      new Color(0xffffff),
      new Color(0xffffff),
  );
  gridHelper.material.opacity = 0.2;
  gridHelper.material.transparent = true;

  boneScene.add(gridHelper);
  const boneHelper = new SkeletonHelper(boneModel);
  boneScene.add(boneHelper);

  boneScene.background = new Color(0x333333);

  defaultAction = defaultMixer.clipAction(defaultModel.animations[0]);
  afterAction = boneMixer.clipAction(boneModel.animations[0]);
  defaultAction.play();
  afterAction.play();
});

const defaultScene = new Scene();
const boneScene = new Scene();
let animationFrameId: number;
defaultScene.add(new AmbientLight(0xffffff, 5));
boneScene.add(new AmbientLight(0xffffff, 5));
// —————————————————————————————————————————————————————————————————————————————————————————————————

const linkTo = (name: string) => {
  isShowCurriculumCenter.value = false;

  if (name == router.currentRoute.value.name) {
    router.replace({name});
  } else {
    router.push({name});
  }
};

onUnmounted(() => {
  cancelAnimationFrame(animationFrameId);
});
const closeMark = () => {
  isShowCurriculumCenter.value = false;
};

const isIn = computed(() => {
  return (name: string) => {
    return router.currentRoute.value.name == name;
  };
});
</script>

<template>
  <div class="center-option" ref="center-option">
    <div
        class="trigger"
        @click="router.replace({ name: 'homeView' })"
        :class="{ active: isIn('homeView') }"
    >
      <span class="iconfont icon-home"/>
      {{ t('home_view.title') }}
    </div>

    <div
        class="trigger"
        @click="router.replace({ name: 'SmartClassView' })"
        :class="{ active: isIn('SmartClassView') }"
    >
        <img class="smart-class-icon" :src="SmartClassImage"/>
        {{ t('smart_class_view.title') }}
    </div>

    <div
        class="trigger"
        @click="showCurriculumCenter"
        :class="{
                active:
                    isIn('aiLearnFriendView') ||
                    isIn('motionDemoView'),
            }"
    >
      <i class="iconfont icon-book-open"></i>
      {{ t('curriculum_center.title') }}
      <i class="iconfont icon-xia"></i>
      <Transition name="mark" type="transition" :duration="500">
        <div
            class="mark"
            v-show="isShowCurriculumCenter"
            @click.stop="closeMark"
        >
          <div class="card">
            <div class="card-icon ai-note"></div>
            <div class="title-and-description">
              <div class="title">AI学习</div>
              <div class="description">
                系统支持实时解析上传的毽球学习音视频，自动转写上传的音视频资料，并在转写基础上提取关键词。
              </div>
            </div>
            <button
                class="go-btn"
                @click="linkTo('aiLearnFriendView')"
            >
              进入页面
              <i class="iconfont icon-right-arrow"></i>
            </button>
          </div>
          <div class="card">
            <div class="card-icon motion-demo">
              <canvas
                  class="motion-demo-default-canvas"
                  style="width: 100%; height: 100%"
              />
              <canvas class="motion-demo-after-canvas"/>
            </div>
            <div class="title-and-description">
              <div class="title">大师示范</div>
              <div class="description">
                采用度量专业动作捕捉设备采集了五名世界冠军运动员的动作数据集。
              </div>
            </div>
            <button
                class="go-btn"
                @click="linkTo('motionDemoView')"
            >
              进入页面
              <i class="iconfont icon-right-arrow"></i>
            </button>
          </div>
        </div>
      </Transition>
    </div>

    <div
        class="trigger"
        @click="linkTo('motionAssessmentView')"
        :class="{ active: isIn('motionAssessmentView') }"
    >
      <img class="personal-assessment-icon" :src="PersonalAssessmentImage"/>
      {{ t('personal_analysis_view.title') }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.center-option {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 45px;
  height: 100%;
  font-size: 16px;

  .trigger {
    cursor: pointer;
    font-size: 16px;
    font-weight: 500;
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 5px;
    color: #777;
    transition: color 0.2s;
    position: relative;

    &:hover {
      color: $blue-7;
    }

    &.active {
      color: $blue-7;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 0;
        width: 100%;
        height: 3px;
        transform: translateY(100%);
        background-color: $blue-5;
      }
    }

    .iconfont {
      font-size: 15px;
    }

    .smart-class-icon {
      width: 25px;
      height: 25px;
    }

    .personal-assessment-icon {
      width: 25px;
      height: 25px;
    }

    .mark {
      position: fixed;
      inset: 0;
      background-color: rgba(51, 51, 51, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 40px;
      transition: opacity 0.3s linear;
      z-index: 1000;

      .card {
        $card-height: 420px;
        $card-width: 340px;
        height: $card-height;
        width: $card-width;
        background-color: rgba(255, 255, 255);
        box-shadow: 0 0 20px rgba(0, 0, 0, 0.2);
        border-radius: 12px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        padding: 16px;
        overflow: hidden;
        color: #1f2020;

        .card-icon {
          height: 55%;
          width: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          overflow: hidden;
          position: relative;
          border-radius: 8px;

          &.base-course {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

            .cube-outer {
              $size: 100px;
              width: $size;
              height: $size;
              transform-style: preserve-3d;
              transform: rotateX(-33.5deg) rotateY(45deg);
              animation: rotate-outer 4s linear infinite;
              display: flex;
              align-items: center;
              justify-content: center;

              @keyframes rotate-outer {
                0% { transform: rotateX(-33.5deg) rotateY(45deg); }
                50% { transform: rotateX(-33.5deg) rotateY(225deg); }
                100% { transform: rotateX(-33.5deg) rotateY(225deg); }
              }

              .plane {
                position: absolute;
                inset: 0;
                border: 1px solid rgba(255, 255, 255);
                backface-visibility: hidden;

                &.top {
                  transform: rotateX(90deg) translateZ(calc($size / 2));
                  background-color: rgba(255, 166, 0, 0.3);
                }
                &.bottom {
                  transform: rotateX(-90deg) translateZ(calc($size / 2));
                  background-color: rgba(255, 0, 221, 0.3);
                }
                &.front {
                  transform: translateZ(calc($size / 2));
                  background-color: rgba(0, 255, 255, 0.3);
                }
                &.back {
                  transform: rotateY(180deg) translateZ(calc($size / 2));
                  background-color: rgba(0, 255, 255, 0.3);
                }
                &.left {
                  transform: rotateY(-90deg) translateZ(calc($size / 2));
                  background-color: rgba(0, 42, 255, 0.3);
                }
                &.right {
                  transform: rotateY(90deg) translateZ(calc($size / 2));
                  background-color: rgba(0, 42, 255, 0.3);
                }
              }

              .cube-inner {
                width: 60px;
                height: 60px;
                transform-style: preserve-3d;
                transform-origin: 30px 30px;
                animation: rotate-inner 4s linear infinite;

                @keyframes rotate-inner {
                  0% { transform: rotateY(0); }
                  50% { transform: rotateY(-360deg); }
                  100% { transform: rotateY(-360deg); }
                }

                .plane {
                  position: absolute;
                  inset: 0;
                  background: rgba(141, 214, 249, 0.6);
                  border: 1px solid rgba(255, 255, 255);
                  backface-visibility: hidden;

                  &.top { transform: rotateX(90deg) translateZ(30px); }
                  &.bottom { transform: rotateX(-90deg) translateZ(30px); }
                  &.front { transform: translateZ(30px); }
                  &.back { transform: rotateY(180deg) translateZ(30px); }
                  &.left { transform: rotateY(-90deg) translateZ(30px); }
                  &.right { transform: rotateY(90deg) translateZ(30px); }
                }
              }
            }
          }

          &.ai-note {
            background-image: url('@/components/数据展示/AiAvatar/智能体头像.gif');
            background-size: cover;
            background-position: center;
          }

          &.motion-demo {
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);

            .motion-demo-after-canvas {
              position: absolute;
              inset: 0;
              width: 100%;
              height: 100%;
              animation: clip-change 7s linear infinite;
            }

            @keyframes clip-change {
              0%, 100% {
                clip-path: polygon(70% 0, 100% 0, 100% 100%, 40% 100%);
              }
              50% {
                clip-path: polygon(40% 0, 100% 0, 100% 100%, 10% 100%);
              }
            }
          }
        }

        .title-and-description {
          flex: 1;
          display: flex;
          flex-direction: column;

          .title {
            font-size: 18px;
            font-weight: 600;
            margin-bottom: 6px;
          }

          .description {
            font-size: 12px;
            color: #666;
            line-height: 1.5;
            overflow: hidden;
            display: -webkit-box;
            -webkit-line-clamp: 3;
            -webkit-box-orient: vertical;
          }
        }

        .go-btn {
          align-self: flex-end;
          display: flex;
          align-items: center;
          gap: 6px;
          padding: 8px 16px;
          background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
          color: white;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          font-size: 14px;
          transition: all 0.3s;

          &:hover {
            transform: translateX(4px);
            box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
          }

          .iconfont {
            font-size: 12px;
          }
        }
      }

      &-enter-active .card:nth-child(2) {
        transition-delay: 0.1s;
      }

      &-leave-active .card:nth-child(2) {
        transition-delay: 0.1s;
      }

      &-enter-from {
        opacity: 0;

        .card {
          opacity: 0;
          transform: translateY(-30px);
        }
      }

      &-leave-to {
        opacity: 0;

        .card {
          opacity: 0;
          transform: translateY(30px);
        }
      }
    }
  }
}
</style>