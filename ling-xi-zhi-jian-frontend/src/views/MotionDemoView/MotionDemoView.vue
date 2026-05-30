<script setup lang="ts">
import {
    AmbientLight,
    AnimationAction,
    AnimationMixer,
    BufferGeometry,
    Clock,
    Color,
    EquirectangularReflectionMapping,
    GridHelper,
    Group,
    Line,
    LineBasicMaterial,
    Mesh,
    MeshPhongMaterial,
    PerspectiveCamera,
    Scene,
    SkeletonHelper,
    SphereGeometry,
    TextureLoader,
    Vector3,
    WebGLRenderer,
} from 'three';
import { computed, nextTick, onMounted, ref, useTemplateRef, watch } from 'vue';
import { ThreeUtil } from '@/utils/ThreeUtil';
import skillData from '@/assets/motion_demo_view/skill_data.json';
import type { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import Header from '@/views/Header/Header.vue';
import MotionItem from '@/views/MotionDemoView/MotionItem.vue';
import Select from '@/components/数据录入/Select.vue';
import { motionOption } from '@/stores/motionAssessmentStore';
import {
    loadMotionCSV,
    motionCSVMapping,
    boneConnections,
    allBoneNames,
    type MotionFrame,
} from '@/utils/MotionCSVUtil';

// 网格地面
const motionDemoViewRef = useTemplateRef<HTMLElement>('motion-demo-view');

let fps = ref(30);

const isPlayingAction = ref(false);
const isPlayingVideo = ref(false);
const currentSliderValue = ref<number>(0); // 滑块位置   单位是帧
const sliderMaxValue = ref<number>(100); // 动画总帧数
const motionAndFileNameMapping = {
    正脚背发球: '正脚背发球.fbx',
    脚外侧发球: '脚外侧发球.fbx',
    大腿触球: '大腿触球.fbx',
    单脚内侧踢球: '单脚内侧踢球.fbx',
    双脚内侧踢球: '双脚内侧踢球.fbx',
    脚外侧踢球: '脚外侧踢球.fbx',
    脚背踢球: '脚背踢球.fbx',
} as {
    [motionName: string]: string;
};

class MyScene {
    static clock = new Clock();
    static currentMotionName = '';
    action: AnimationAction | null = null;
    mixer: AnimationMixer | null = null;
    model: Group | null = null;
    controls: OrbitControls | null = null;
    scene = new Scene();
    renderer: WebGLRenderer | null = null;
    camera: PerspectiveCamera | null = null;
    skeletonHelper: SkeletonHelper | null = null;

    constructor(canvas: HTMLCanvasElement) {
        this.camera = ThreeUtil.createCamera(canvas);
        this.renderer = ThreeUtil.createWebGLRenderer(canvas);
        this.renderer.setSize(canvas.offsetWidth, canvas.offsetHeight);
        this.controls = ThreeUtil.createControls(this.camera, canvas);
        this.camera.position.set(0, 200, 500);
        this.camera.lookAt(0, 20, 0);

        this.addAmbientLight();
    }

    createAnimationMixer() {
        this.mixer = new AnimationMixer(this.model!);
    }

    createAction() {
        this.action = this.mixer!.clipAction(this.model!.animations[0]);
    }

    playAction() {
        this.action?.reset().fadeIn(0).play();
    }

    removeModelFromScene() {
        if (this.model) {
            this.scene.remove(this.model);
        }
    }

    loadModel(motionName: string) {
        return new Promise<void>((resolve, reject) => {
            isLoading.value = true;
            modelLoadingProgress.value = 0;
            ThreeUtil.loadFBX(
                '/motion_demo_view/model/motion/' +
                    motionAndFileNameMapping[motionName],
                (xhr) => {
                    modelLoadingProgress.value = parseFloat(
                        ((xhr.loaded / xhr.total) * 100).toFixed(2),
                    );
                },
            )
                .then((model: Group) => {
                    modelLoadingProgress.value = 100;
                    // data.scale.set(0.01, 0.01, 0.01);
                    model.castShadow = true;
                    this.model = model;
                    MyScene.currentMotionName = motionName; // 记录当前动作名称
                    resolve();
                })
                .catch((err) => {
                    reject(err);
                })
                .finally(() => {
                    isLoading.value = false;
                });
        });
    }

    loadBone(motionName: string) {
        return new Promise<void>((resolve, reject) => {
            isLoading.value = true;
            modelLoadingProgress.value = 0;
            ThreeUtil.loadFBX(
                '/motion_demo_view/model/bone/' +
                    motionAndFileNameMapping[motionName],
                (xhr: { loaded: number; total: number }) => {
                    modelLoadingProgress.value = parseFloat(
                        ((xhr.loaded / xhr.total) * 100).toFixed(2),
                    );
                },
            )
                .then((model: Group) => {
                    modelLoadingProgress.value = 100;
                    // data.scale.set(0.01, 0.01, 0.01);
                    this.model = model;
                    MyScene.currentMotionName = motionName; // 记录当前动作名称
                    resolve();
                })
                .catch((err) => {
                    reject(err);
                })
                .finally(() => {
                    isLoading.value = false;
                });
        });
    }

    showSkeletonHelperToScene() {
        if (this.skeletonHelper) {
            this.scene.remove(this.skeletonHelper);
        }
        // 显示骨骼
        this.skeletonHelper = new SkeletonHelper(this.model!);
        this.scene.add(this.skeletonHelper);
    }

    /**
     * 添加模型到场景
     */
    addModelToScene() {
        this.scene.add(this.model!);
    }

    /**
     * 添加环境贴图
     */
    addEnvMapTexture() {
        // 将渐变纹理设置为场景的环境贴图和背景
        const textureLoader = new TextureLoader();
        const envMapTexture = textureLoader.load(
            '/motion_demo_view/model/scene/EnvMapTexture.jpg',
        );
        envMapTexture.mapping = EquirectangularReflectionMapping; //正常只是一张图平铺，设置这个可以让图包围环绕整个环境
        this.scene.environment = envMapTexture;
        this.scene.background = envMapTexture;
    }

    /**
     * 设置场景背景色
     * @param color
     */
    setBackgroundColor(color: Color) {
        this.scene.background = color;
    }

    /**
     * 添加环境光
     */
    addAmbientLight() {
        const ambientLight = new AmbientLight(0xffffff, 4);
        this.scene.add(ambientLight);
    }

    addGridHelper() {
        const gridHelper = new GridHelper(10000, 200);
        gridHelper.material.opacity = 0.2;
        gridHelper.material.transparent = true;
        this.scene.add(gridHelper);
    }

    /**
     * 添加地面
     */
    addFloor() {
        ThreeUtil.loadGlb('/motion_demo_view/model/scene/羽毛球场.glb').then(
            (model) => {
                const floor = model.scenes[0];
                floor.castShadow = true;
                floor.scale.set(70, 70, 70);
                this.scene.add(floor);
            },
        );
    }
}

class CSVSkeletonScene {
    static clock = new Clock();
    scene: Scene;
    renderer: WebGLRenderer | null = null;
    camera: PerspectiveCamera | null = null;
    controls: OrbitControls | null = null;
    skeletonGroup: Group | null = null;
    boneMeshes: Map<string, Mesh> = new Map();
    boneLines: Map<string, Line> = new Map();

    constructor(canvas: HTMLCanvasElement) {
        this.scene = new Scene();
        this.scene.background = new Color(0x333333);
        this.camera = ThreeUtil.createCamera(canvas);
        this.renderer = new WebGLRenderer({
            canvas: canvas,
            antialias: true,
            alpha: false,
        });
        this.renderer.setSize(canvas.offsetWidth, canvas.offsetHeight);
        this.controls = ThreeUtil.createControls(this.camera, canvas);
        this.camera.position.set(0, 200, 500);
        this.camera.lookAt(0, 20, 0);

        const ambientLight = new AmbientLight(0xffffff, 4);
        this.scene.add(ambientLight);

        const gridHelper = new GridHelper(10000, 200);
        gridHelper.material.opacity = 0.2;
        gridHelper.material.transparent = true;
        this.scene.add(gridHelper);

        this.skeletonGroup = new Group();
        this.scene.add(this.skeletonGroup);
        this.createSkeleton();
    }

    createSkeleton() {
        if (!this.skeletonGroup) return;
        this.skeletonGroup.clear();
        this.boneMeshes.clear();
        this.boneLines.clear();

        const boneGeometry = new SphereGeometry(15, 16, 16);
        const boneMaterial = new MeshPhongMaterial({ color: 0x00ff88 });

        allBoneNames.forEach(boneName => {
            const boneMesh = new Mesh(boneGeometry, boneMaterial.clone());
            boneMesh.name = boneName;
            boneMesh.position.set(0, 0, 0);
            this.skeletonGroup!.add(boneMesh);
            this.boneMeshes.set(boneName, boneMesh);
        });

        const lineMaterial = new LineBasicMaterial({ color: 0x00ff88, linewidth: 2 });
        boneConnections.forEach(([parentName, childName]) => {
            const lineGeometry = new BufferGeometry().setFromPoints([
                new Vector3(0, 0, 0),
                new Vector3(0, 50, 0),
            ]);
            const line = new Line(lineGeometry, lineMaterial);
            line.name = `${parentName}-${childName}`;
            this.skeletonGroup!.add(line);
            this.boneLines.set(`${parentName}-${childName}`, line);
        });
    }

    updateSkeleton(frame: MotionFrame) {
        if (!this.skeletonGroup) return;

        this.boneMeshes.forEach((mesh, boneName) => {
            if (frame.bones.has(boneName)) {
                const boneData = frame.bones.get(boneName)!;
                mesh.position.copy(boneData.position);
                mesh.position.y = -mesh.position.y + 1000;
                mesh.position.z = -mesh.position.z;
                mesh.quaternion.copy(boneData.rotation);
            }
        });

        this.boneLines.forEach((line, connectionName) => {
            const [parentName, childName] = connectionName.split('-');
            if (parentName && childName && frame.bones.has(parentName) && frame.bones.has(childName)) {
                const parentPos = frame.bones.get(parentName)!.position.clone();
                const childPos = frame.bones.get(childName)!.position.clone();

                parentPos.y = -parentPos.y + 1000;
                parentPos.z = -parentPos.z;
                childPos.y = -childPos.y + 1000;
                childPos.z = -childPos.z;

                const positions = line.geometry.attributes.position.array as Float32Array;
                positions[0] = parentPos.x;
                positions[1] = parentPos.y;
                positions[2] = parentPos.z;
                positions[3] = childPos.x;
                positions[4] = childPos.y;
                positions[5] = childPos.z;
                line.geometry.attributes.position.needsUpdate = true;
            }
        });
    }

    render() {
        if (this.renderer && this.camera) {
            this.renderer.render(this.scene, this.camera);
        }
    }
}

const isLoading = ref(false);
const modelLoadingProgress = ref<number>(0);
const singlePersonMotions = ['绷踢', '盘踢', '磕踢', '对踢', '拐踢', '头触球', '跳踢', '胸触球'];
const isCSVMode = computed(() => singlePersonMotions.includes(selectedMotion.value));
let csvSkeletonScene: CSVSkeletonScene | null = null;
let motionData: MotionFrame[] = [];
let csvCurrentFrame = 0;

/**
 * 选择动作时触发,
 * 当切换 动作时
 * 1. 加载对应资源
 * 3. 播放 资源
 * 4。确定  isPlayingVideo.value 和 isPlayingAction.value
 * @param motionName
 */
const playMotion = (motionName: string) => {
    isPlayingVideo.value = false;
    isPlayingAction.value = false;
    
    if (isCSVMode.value) {
        // 隐藏 FBX 模型
        defaultScene?.removeModelFromScene();
        boneScene?.removeModelFromScene();
        
        if (motionData.length > 0) {
            csvCurrentFrame = 0;
            csvSkeletonScene?.updateSkeleton(motionData[0]);
            isPlayingAction.value = true;
            sliderMaxValue.value = motionData.length;
            currentSliderValue.value = 0;
        }
        return;
    }
    
    if (displayMode.value == 'livaVideo') {
        // 处于视频模式时
        // 已经切换了 src，只需要在 下一个任务 去执行 3，4 就好
        const liveVideo = motionDemoViewRef.value?.querySelector(
            '.live-video',
        ) as HTMLVideoElement;
        nextTick(() => {
            liveVideo.play().then(() => {
                isPlayingVideo.value = true;
            });
        });
    } else {
        // 删除之前的模型
        defaultScene?.removeModelFromScene();
        boneScene?.removeModelFromScene();
        // 同时加载模型
        Promise.all([
            defaultScene?.loadModel(motionName),
            boneScene?.loadBone(motionName),
        ]).then(() => {
            // 创建动画混合器
            boneScene?.createAnimationMixer();
            defaultScene?.createAnimationMixer();

            // 创建骨骼辅助器
            boneScene?.showSkeletonHelperToScene();

            boneScene?.addModelToScene();
            defaultScene?.addModelToScene();

            boneScene?.createAction();
            defaultScene?.createAction();

            // 播放动画
            defaultScene?.playAction();
            boneScene?.playAction();

            isPlayingAction.value = true;
        });
    }
};

// 播放和暂停逻辑
const togglePlayPause = () => {
    if (displayMode.value == 'livaVideo') {
        // 处于视频模式时
        isPlayingVideo.value = !isPlayingVideo.value;
        const liveVideo = motionDemoViewRef.value?.querySelector(
            '.live-video',
        ) as HTMLVideoElement;
        if (isPlayingVideo.value) {
            liveVideo.play();
        } else {
            liveVideo.pause();
        }
    } else if (isCSVMode.value) {
        isPlayingAction.value = !isPlayingAction.value;
    } else {
        isPlayingAction.value = !isPlayingAction.value;
        defaultScene!.action!.paused = boneScene!.action!.paused =
            !isPlayingAction.value;
    }
};

// 使用滑块控制播放进度
const updateSliderValue = () => {
    // 必须全停，因为他们共用一个进度条
    isPlayingVideo.value = false;
    isPlayingAction.value = false;
    
    if (isCSVMode.value) {
        if (motionData.length > 0) {
            csvCurrentFrame = Math.min(currentSliderValue.value, motionData.length - 1);
            csvSkeletonScene?.updateSkeleton(motionData[csvCurrentFrame]);
        }
        return;
    }
    
    defaultScene!.action!.paused = true;
    boneScene!.action!.paused = true;
    const liveVideo = motionDemoViewRef.value?.querySelector(
        '.live-video',
    ) as HTMLVideoElement;
    liveVideo.pause();

    if (displayMode.value == 'livaVideo') {
        // 处于视频模式时
        liveVideo.currentTime = currentSliderValue.value;
    } else {
        boneScene!.action!.time = defaultScene!.action!.time =
            currentSliderValue.value / fps.value;
    }
};

let defaultScene: MyScene | null = null;
let boneScene: MyScene | null = null;
let controllerScene: MyScene | null = null;
onMounted(() => {
    defaultScene = new MyScene(
        motionDemoViewRef.value?.querySelector(
            '.default-canvas',
        ) as HTMLCanvasElement,
    );
    boneScene = new MyScene(
        motionDemoViewRef.value?.querySelector(
            '.after-canvas',
        ) as HTMLCanvasElement,
    );
    controllerScene = new MyScene(
        motionDemoViewRef.value?.querySelector(
            '.controller-canvas',
        ) as HTMLCanvasElement,
    );
    csvSkeletonScene = new CSVSkeletonScene(
        motionDemoViewRef.value?.querySelector(
            '.after-canvas',
        ) as HTMLCanvasElement,
    );
    defaultScene.addFloor();
    defaultScene.addEnvMapTexture();
    boneScene.addGridHelper();
    boneScene.setBackgroundColor(new Color(0x333333));

    if (isCSVMode.value) {
        loadCSVData(selectedMotion.value);
    }

    playMotion(selectedMotion.value);

    const liveVideo = motionDemoViewRef.value?.querySelector(
        '.live-video',
    ) as HTMLVideoElement;

    // 同步两个场景的控制器（观察者模式）
    controllerScene?.controls?.addEventListener('change', () => {
        defaultScene?.controls?.object.copy(controllerScene?.controls?.object!);
        boneScene?.controls?.object.copy(controllerScene?.controls?.object!);
        csvSkeletonScene?.controls?.object.copy(controllerScene?.controls?.object!);
    });

    function setFPS(_fps: number) {
        fps.value = _fps;
    }

    let count = 0;
    let prevTimestamp = 0;

    function animate(timestamp: number) {
        prevTimestamp = timestamp;
        count++;
        // 间隔超过 1s，将之前计算的 count 输出
        if (timestamp - prevTimestamp >= 1000) {
            setFPS(count);
            count = 0;
        }

        const delta = MyScene.clock.getDelta();
        defaultScene?.mixer?.update(delta);
        boneScene?.mixer?.update(delta);
        
        if (isCSVMode.value && isPlayingAction.value && motionData.length > 0) {
            csvCurrentFrame = (csvCurrentFrame + 1) % motionData.length;
            csvSkeletonScene?.updateSkeleton(motionData[csvCurrentFrame]);
            currentSliderValue.value = csvCurrentFrame;
            sliderMaxValue.value = motionData.length;
        }
        
        if (isPlayingAction.value && !isCSVMode.value) {
            currentSliderValue.value = defaultScene!.action!.time * fps.value;
            sliderMaxValue.value =
                defaultScene!.action!.getClip().duration * fps.value;
            // 只有加了这个才能保证同步，注意注意
            boneScene!.action!.time = defaultScene!.action!.time;
        }
        if (isPlayingVideo.value) {
            currentSliderValue.value = liveVideo.currentTime;
            sliderMaxValue.value = liveVideo.duration;
        }
        defaultScene?.controls?.update();
        boneScene?.controls?.update();
        csvSkeletonScene?.controls?.update();
        
        if (isCSVMode.value) {
            // CSV 模式只渲染骨骼场景
            csvSkeletonScene?.render();
        } else {
            defaultScene?.renderer?.render(
                defaultScene.scene,
                defaultScene.camera!,
            );
            boneScene?.renderer?.render(boneScene.scene, boneScene.camera!);
        }
        requestAnimationFrame((timestamp) => animate(timestamp));
    }

    animate(new Date().getTime());
});

const loadCSVData = async (motionName: string) => {
    const csvPath = motionCSVMapping[motionName];
    console.log('Loading CSV for:', motionName, 'Path:', csvPath);
    if (csvPath) {
        isLoading.value = true;
        modelLoadingProgress.value = 30;
        try {
            motionData = await loadMotionCSV(csvPath);
            console.log('CSV loaded, frames:', motionData.length);
            modelLoadingProgress.value = 100;
            if (motionData.length > 0) {
                csvSkeletonScene?.updateSkeleton(motionData[0]);
                sliderMaxValue.value = motionData.length;
                currentSliderValue.value = 0;
                isPlayingAction.value = true;
            }
        } catch (error) {
            console.error('Failed to load CSV motion data:', error);
        } finally {
            isLoading.value = false;
        }
    }
};

const selectedMotion = ref('脚背踢球');
const isShowBoneCanvas = ref(true);
const isShowLiveVideo = ref(false);
type DisplayMode = 'livaVideo' | 'original' | 'glorified';
const displayMode = ref<DisplayMode>('original');
/**
 * 当模式切换时 要做两件事。
 * 一
 *    确定 进度条 的 当前值 和 最大值
 *
 * 二
 *    停止上一个模式
 *
 * 三
 *    确定 isShowLiveVideo.value
 *
 * 四
 *    当选择播放的是模型时需要判断模型的动作和视频的动作是不是同一个
 */
watch(displayMode, (newVal) => {
    const liveVideo = motionDemoViewRef.value?.querySelector(
        '.live-video',
    ) as HTMLVideoElement;
    if (newVal == 'livaVideo') {
        // 如果打开真人视频
        sliderMaxValue.value = liveVideo.duration;
        currentSliderValue.value = liveVideo.currentTime;

        liveVideo.play();
        // boneScene!.action!.paused = true;
        // defaultScene!.action!.paused = true;

        // 确认显示状态
        isShowLiveVideo.value = true;

        // 确认播放状态
        isPlayingVideo.value = true;
        isPlayingAction.value = false;
    } else {
        // 如果打开 3D模型
        // 确认显示状态
        isShowLiveVideo.value = false;

        const liveVideo = motionDemoViewRef.value?.querySelector(
            '.live-video',
        ) as HTMLVideoElement;
        liveVideo.pause();
        isPlayingVideo.value = false;

        // 当选择 的 播放 模式 是 模型时，判断 当前动作 和 选择的动作 是否 为同一个
        if (MyScene.currentMotionName != selectedMotion.value) {
            // 如果不是同一个，那么得重新加载模型。

            // 删除之前的模型
            defaultScene?.removeModelFromScene();
            boneScene?.removeModelFromScene();

            // 停止播放
            isPlayingAction.value = false;

            // 同时加载模型
            Promise.all([
                defaultScene?.loadModel(selectedMotion.value),
                boneScene?.loadBone(selectedMotion.value),
            ]).then(() => {
                // 创建动画混合器
                boneScene?.createAnimationMixer();
                defaultScene?.createAnimationMixer();

                // 创建骨骼辅助器
                boneScene?.showSkeletonHelperToScene();

                boneScene?.addModelToScene();
                defaultScene?.addModelToScene();

                boneScene?.createAction();
                defaultScene?.createAction();

                sliderMaxValue.value =
                    defaultScene!.action!.getClip().duration * fps.value;
                currentSliderValue.value = 0;

                // 播放动画
                defaultScene?.playAction();
                boneScene?.playAction();

                isPlayingAction.value = true;
            });
        } else {
            // 如果选择的是同一个动作，那么直接播放即可
            sliderMaxValue.value =
                defaultScene!.action!.getClip().duration * fps.value;
            currentSliderValue.value = defaultScene!.action!.time * fps.value;
            // 播放动画
            defaultScene?.playAction();
            boneScene?.playAction();
            isPlayingAction.value = true;
        }

        isShowBoneCanvas.value = newVal == 'original';
    }
});

watch(selectedMotion, (newMotion) => {
    if (isCSVMode.value) {
        // 隐藏 FBX 模型
        defaultScene?.removeModelFromScene();
        boneScene?.removeModelFromScene();
        
        loadCSVData(newMotion).then(() => {
            if (motionData.length > 0) {
                csvCurrentFrame = 0;
                csvSkeletonScene?.updateSkeleton(motionData[0]);
                sliderMaxValue.value = motionData.length;
                currentSliderValue.value = 0;
                isPlayingAction.value = true;
            }
        });
    }
});

interface SkillData {
    name: string;
    function: string;
    motionPoint: string;
    step: string[];
}

const getFunction = computed(() => {
    return (skillData as SkillData[]).find(
        (skill) => skill.name === selectedMotion.value,
    )?.function;
});
const getMotionPoint = computed(() => {
    return (skillData as SkillData[]).find(
        (skill) => skill.name === selectedMotion.value,
    )?.motionPoint;
});
const getStep = computed(() => {
    return (skillData as SkillData[]).find(
        (skill) => skill.name === selectedMotion.value,
    )?.step;
});

const isPlaying = computed(() => {
    if (displayMode.value == 'livaVideo') {
        return isPlayingVideo.value;
    } else {
        return isPlayingAction.value;
    }
});
const displayModeOption = [
    {
        label: '真人动作',
        value: 'livaVideo',
    },
    {
        label: '3D骨骼动作',
        value: 'original',
    },
    {
        label: '3D人体动作',
        value: 'glorified',
    },
];
</script>

<template>

    



    <div class="motion-demo-view" ref="motion-demo-view">
        <Header show-back />
        <div class="main">
            <div class="left">
                <div class="left-box">
                    <a-card
                        class="motion-option-card"
                        size="small"
                        v-for="(item, key, index) in motionOption"
                        :key="index"
                        :title="key"
                        :bordered="false"
                    >
                        <MotionItem
                            :item="item"
                            v-model:selectedMotion="selectedMotion"
                            @change="playMotion"
                        />
                    </a-card>
                </div>
            </div>
            <div class="right">
                <div class="display-box">
                    <div class="model-option">
                        <Select
                            v-model="displayMode"
                            :items="displayModeOption"
                        />
                    </div>
                    <video
                        class="live-video"
                        v-show="isShowLiveVideo"
                        loop
                        muted
                        :src="
                            '/motion_demo_view/真人演示/' +
                            selectedMotion +
                            '.mp4'
                        "
                        type="video/mp4"
                    />
                    <div class="canvas-container" v-show="!isShowLiveVideo">
                        <canvas class="default-canvas" />
                        <Transition
                            name="after-canvas"
                            type="animation"
                            mode="out-in"
                        >
                            <canvas
                                class="after-canvas"
                                v-show="isShowBoneCanvas"
                            />
                        </Transition>
                        <canvas class="controller-canvas" />
                    </div>
                    <div class="mark" v-show="isLoading">
                        <a-progress
                            :percent="modelLoadingProgress"
                            type="circle"
                            :size="120"
                        />
                        <p>加载中...</p>
                    </div>
                </div>

                <div class="right-panel">
                    <div class="right-panel-box">
                        <h3 class="skill-name">
                            动作名称: {{ selectedMotion }}
                        </h3>
                        <h4>功能</h4>
                        <p class="skill-function">{{ getFunction }}</p>
                        <h4>要点</h4>
                        <p class="skill-point">{{ getMotionPoint }}</p>
                        <h4>步骤</h4>
                        <div class="skill-step-list">
                            <div
                                class="skill-step"
                                v-for="(item, index) of getStep"
                                :key="index"
                            >
                                {{ item }}
                            </div>
                        </div>
                    </div>
                </div>

                <div class="footer-panel">
                    <button class="control-button" @click="togglePlayPause">
                        <span class="iconfont icon-ai07" v-if="isPlaying" />
                        <span class="iconfont icon-bofang" v-else />
                    </button>
                    <a-slider
                        v-model:value="currentSliderValue"
                        :max="sliderMaxValue"
                        class="progress-slider"
                        @change="updateSliderValue"
                    />
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.motion-demo-view {
    height: 100vh;
    width: 100vw;
    display: flex;
    flex-direction: column;
    background-color: #f3f5fb;

    .main {
        flex: 1;
        display: flex;
        overflow: hidden;
        flex-direction: row;
        justify-content: space-between;
    }

    $leftWidth: 300px;

    .left {
        width: $leftWidth;
        overflow: auto;

        &::-webkit-scrollbar {
            display: none;
        }

        @include useMediaQuery(
            phone,
            (
                display: none,
            )
        );

        .left-box {
            margin: $gap;
            display: flex;
            flex-direction: column;
            gap: $gap;

            .motion-option-card {
                box-shadow: $box-shadow;
            }
        }
    }

    .right {
        flex: 1;
        display: grid;
        grid-template-columns: 1fr auto;
        grid-template-rows: 1fr 90px;
        grid-template-areas:
            'box right'
            'footer right';
        gap: $gap;

        .display-box {
            margin-top: $gap;
            grid-area: box;
            position: relative;
            overflow: hidden;
            box-shadow: $box-shadow;
            border-radius: 10px;
            background-color: #fff;

            .mark {
                position: absolute;
                inset: 0;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                gap: 20px;
                background-color: rgba(255, 255, 255, 0.5);
                backdrop-filter: blur(10px);
            }

            .model-option {
                position: absolute;
                top: 5px;
                left: 5px;
                display: flex;
                flex-direction: row;
                gap: 5px;
                z-index: 1;

                .select {
                    background-color: rgba(255, 255, 255, 0.5);
                }
            }

            .live-video {
                display: block;
                height: 100%;
                width: 100%;
                object-fit: fill;
            }

            .canvas-container {
                height: 100%;
                width: 100%;
                position: relative;

                canvas {
                    height: 100%;
                    width: 100%;
                }

                .after-canvas {
                    position: absolute;
                    inset: 0;
                    overflow: hidden;

                    &-leave-active {
                        animation: hidden 3s linear forwards;
                    }

                    &-enter-active {
                        animation: hidden 3s linear forwards reverse;
                    }

                    @keyframes hidden {
                        0% {
                            clip-path: circle(150% at 0 0);
                        }
                        100% {
                            clip-path: circle(0 at 0 0);
                        }
                    }
                }

                .controller-canvas {
                    position: absolute;
                    inset: 0;
                }
            }
        }

        .right-panel {
            grid-area: right;
            overflow-y: auto;

            &::-webkit-scrollbar {
                display: none;
            }

            .right-panel-box {
                width: $leftWidth;
                background: rgba(255, 255, 255, 0.9);
                backdrop-filter: blur(20px);
                border-radius: 16px;
                padding: 24px;
                overflow-y: auto;
                box-shadow: 0 8px 32px rgba(24, 144, 255, 0.12);
                border: 1px solid rgba(24, 144, 255, 0.15);
                margin-right: $gap;
                margin-top: $gap;
                margin-bottom: $gap;
                min-height: calc(100% - $gap * 2);
                position: relative;
                overflow: hidden;

                &::before {
                    content: '';
                    position: absolute;
                    top: 0;
                    left: 0;
                    right: 0;
                    height: 120px;
                    background: linear-gradient(135deg, rgba(24, 144, 255, 0.08) 0%, rgba(105, 192, 255, 0.04) 100%);
                    pointer-events: none;
                }

                .skill-name {
                    font-size: 1.4rem;
                    font-weight: 700;
                    margin-bottom: 20px;
                    color: #1890ff;
                    border-bottom: 2px solid #1890ff;
                    padding-bottom: 12px;
                    position: relative;
                    z-index: 1;
                    text-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
                }

                h4 {
                    font-size: 0.95rem;
                    font-weight: 600;
                    color: #333;
                    margin: 16px 0 8px 0;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    position: relative;
                    z-index: 1;

                    &::before {
                        content: '';
                        width: 4px;
                        height: 16px;
                        background: linear-gradient(180deg, #1890ff 0%, #69c0ff 100%);
                        border-radius: 2px;
                    }
                }

                .skill-function,
                .skill-point {
                    font-size: 0.9rem;
                    color: #666;
                    line-height: 1.7;
                    padding: 12px 16px;
                    background: linear-gradient(135deg, rgba(24, 144, 255, 0.06) 0%, rgba(105, 192, 255, 0.03) 100%);
                    border-radius: 10px;
                    border-left: 3px solid #69c0ff;
                    position: relative;
                    z-index: 1;
                }

                .skill-step-list {
                    display: flex;
                    flex-direction: column;
                    gap: 10px;
                    position: relative;
                    z-index: 1;

                    .skill-step {
                        background: linear-gradient(135deg, rgba(24, 144, 255, 0.1) 0%, rgba(105, 192, 255, 0.05) 100%);
                        border: 1px solid rgba(24, 144, 255, 0.15);
                        border-radius: 10px;
                        margin: 0;
                        padding: 12px 16px;
                        font-size: 0.88rem;
                        color: #444;
                        line-height: 1.6;
                        transition: all 0.3s;
                        position: relative;
                        padding-left: 40px;

                        &::before {
                            content: counter(step);
                            counter-increment: step;
                            position: absolute;
                            left: 12px;
                            top: 50%;
                            transform: translateY(-50%);
                            width: 22px;
                            height: 22px;
                            background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                            border-radius: 50%;
                            color: #fff;
                            font-size: 0.75rem;
                            font-weight: 600;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                        }

                        &:hover {
                            transform: translateX(4px);
                            background: linear-gradient(135deg, rgba(24, 144, 255, 0.15) 0%, rgba(105, 192, 255, 0.08) 100%);
                            box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
                        }
                    }

                    counter-reset: step;
                }
            }
        }

        .footer-panel {
            grid-area: footer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 25px;
            padding: 20px;
            box-shadow: $box-shadow;
            background-color: #fff;
            border-radius: 10px;
            margin-bottom: $gap;

            .iconfont {
                font-size: 20px;
                color: #fff;
            }

            .control-button {
                border: none;
                font-size: 1.6rem;
                cursor: pointer;
                color: #5488d6;
                transition: transform 0.3s ease;
                display: flex;
                align-items: center;
                justify-content: center;
                width: 40px;
                height: 40px;
                border-radius: 50%;
                background-color: #b7d8ff;

                &:hover {
                    transform: scale(1.3);
                    background-color: #5488d6;
                    color: white;
                }
            }

            .progress-slider {
                flex: 1;
                max-width: 320px;
            }
        }
    }
}
</style>
