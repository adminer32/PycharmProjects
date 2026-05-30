<script setup lang="ts">
import Button from '@/components/通用/Button/Button.vue';
import { onMounted, onUnmounted, useTemplateRef } from 'vue';
import { useRouter } from 'vue-router';
import {
    AmbientLight,
    AnimationMixer,
    Clock,
    Color,
    EquirectangularReflectionMapping,
    GridHelper,
    Scene,
    SkeletonHelper,
    TextureLoader,
} from 'three';
import { ThreeUtil } from '@/utils/ThreeUtil';
import SlidingWindow from '@/components/数据展示/SlidingWindow.vue';

const actionDemoRef = useTemplateRef<HTMLElement>('action-demo');
const router = useRouter();
const clock = new Clock();

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
        texture.mapping = EquirectangularReflectionMapping; //正常只是一张图平铺，设置这个可以让图包围环绕整个环境
        defaultScene.environment = texture;
        defaultScene.background = texture;
    });
let defaultMixer: AnimationMixer;
let boneMixer: AnimationMixer;

const defaultScene = new Scene();
const boneScene = new Scene();
let animationFrameId: number;
defaultScene.add(new AmbientLight(0xffffff, 5));
boneScene.add(new AmbientLight(0xffffff, 5));
onMounted(() => {
    Promise.all([
        ThreeUtil.loadFBX('/motion_demo_view/model/motion/大腿触球.fbx'),
        ThreeUtil.loadFBX('/motion_demo_view/model/bone/大腿触球.fbx'),
    ]).then((res) => {
        const defaultCanvas = actionDemoRef.value!.querySelector(
            '.default-canvas',
        ) as HTMLCanvasElement;
        const afterCanvas = actionDemoRef.value!.querySelector(
            '.after-canvas',
        ) as HTMLCanvasElement;

        const defaultCamera = ThreeUtil.createCamera(defaultCanvas);
        const defaultRenderer = ThreeUtil.createWebGLRenderer(defaultCanvas);
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

        const defaultModel = res[0];
        defaultModel.castShadow = true;
        defaultMixer = new AnimationMixer(defaultModel);
        defaultScene.add(defaultModel);

        // after
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

        // 世界网格
        boneScene.add(gridHelper);
        // 显示骨骼
        const boneHelper = new SkeletonHelper(boneModel);
        boneScene.add(boneHelper);

        // 添加背景
        boneScene.background = new Color(0x333333);

        const defaultAction = defaultMixer.clipAction(
            defaultModel.animations[0],
        );
        const afterAction = boneMixer.clipAction(boneModel.animations[0]);
        // 播放
        defaultAction.play();
        afterAction.play();

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
});

onUnmounted(() => {
    cancelAnimationFrame(animationFrameId);
});
</script>

<template>
    <div class="action-demo" ref="action-demo">
        <div class="left">
            <img
                class="logo"
                src="@/assets/home_view/ThreeBoxContainer/basic_course.png"
                alt=""
            />
            <div class="title">
                3D动作演示<br />
                提供全方位动作演示，边看边练
            </div>
            <p>
                通过捕捉标准动作，从慢放临摹到动态校准，让每个腾空转身都自带教练级反馈，助你练就肌肉记忆级的标准动作范式。
            </p>
            <Button @click="router.push({ name: 'motionDemoView' })"
                >了解更多</Button
            >
        </div>

        <SlidingWindow class="right">
            <canvas class="default-canvas"></canvas>
            <template #after>
                <canvas class="after-canvas"></canvas>
            </template>
        </SlidingWindow>
    </div>
</template>

<style scoped lang="scss">
@use 'variable';

.action-demo {
    @include variable.style;

    .right {
        overflow: hidden;
        display: flex;
        flex-direction: column;
        justify-content: center;
        border-radius: $border-radius;

        canvas {
            width: 100%;
            height: 100%;
        }
    }
}
</style>
