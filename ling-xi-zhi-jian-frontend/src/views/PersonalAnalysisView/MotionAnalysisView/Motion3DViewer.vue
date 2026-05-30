<template>
    <div ref="containerRef" class="motion-3d-viewer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';

interface MotionFrame {
    bones: Map<string, { position: THREE.Vector3; rotation: THREE.Quaternion }>;
}

const props = defineProps<{
    csvUrl?: string;
}>();

const containerRef = ref<HTMLDivElement | null>(null);
let scene: THREE.Scene;
let camera: THREE.PerspectiveCamera;
let renderer: THREE.WebGLRenderer;
let controls: OrbitControls;
let skeleton: THREE.Group;
let animationId: number;
let motionData: MotionFrame[] = [];
let currentFrame = 0;
let isPlaying = true;

const boneConnections = [
    ['Hips', 'Spine'],
    ['Spine', 'Spine1'],
    ['Spine1', 'Spine2'],
    ['Spine2', 'Spine3'],
    ['Spine3', 'LeftShoulder'],
    ['LeftShoulder', 'LeftArm'],
    ['LeftArm', 'LeftForeArm'],
    ['LeftForeArm', 'LeftHand'],
    ['Spine3', 'RightShoulder'],
    ['RightShoulder', 'RightArm'],
    ['RightArm', 'RightForeArm'],
    ['RightForeArm', 'RightHand'],
    ['Spine3', 'Neck'],
    ['Neck', 'Head'],
    ['Hips', 'LeftUpLeg'],
    ['LeftUpLeg', 'LeftLeg'],
    ['LeftLeg', 'LeftFoot'],
    ['LeftFoot', 'LeftToeBase'],
    ['Hips', 'RightUpLeg'],
    ['RightUpLeg', 'RightLeg'],
    ['RightLeg', 'RightFoot'],
    ['RightFoot', 'RightToeBase'],
];

const initScene = () => {
    if (!containerRef.value) return;

    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x1a1a2e);

    camera = new THREE.PerspectiveCamera(
        60,
        containerRef.value.clientWidth / containerRef.value.clientHeight,
        0.1,
        10000
    );
    camera.position.set(0, 500, 1000);

    renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setSize(containerRef.value.clientWidth, containerRef.value.clientHeight);
    renderer.setPixelRatio(window.devicePixelRatio);
    containerRef.value.appendChild(renderer.domElement);

    controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.05;
    controls.minDistance = 200;
    controls.maxDistance = 3000;
    controls.target.set(0, 400, 0);

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
    scene.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
    directionalLight.position.set(500, 1000, 500);
    scene.add(directionalLight);

    const gridHelper = new THREE.GridHelper(2000, 20, 0x444444, 0x222222);
    gridHelper.position.y = 0;
    scene.add(gridHelper);

    skeleton = new THREE.Group();
    scene.add(skeleton);

    createSkeleton();
};

const createSkeleton = () => {
    skeleton.clear();

    const boneGeometry = new THREE.SphereGeometry(15, 16, 16);
    const boneMaterial = new THREE.MeshPhongMaterial({ color: 0x00ff88 });

    boneConnections.forEach(([parentName, childName]) => {
        const parentBone = new THREE.Mesh(boneGeometry, boneMaterial.clone());
        parentBone.name = parentName;
        parentBone.position.set(0, 0, 0);
        skeleton.add(parentBone);

        const childBone = new THREE.Mesh(boneGeometry, boneMaterial.clone());
        childBone.name = childName;
        childBone.position.set(0, 0, 0);
        skeleton.add(childBone);

        const lineGeometry = new THREE.BufferGeometry().setFromPoints([
            new THREE.Vector3(0, 0, 0),
            new THREE.Vector3(0, 50, 0),
        ]);
        const lineMaterial = new THREE.LineBasicMaterial({ color: 0x00ff88, linewidth: 2 });
        const line = new THREE.Line(lineGeometry, lineMaterial);
        line.name = `${parentName}-${childName}`;
        skeleton.add(line);
    });
};

const parseCSV = (csvText: string) => {
    const lines = csvText.split('\n');
    const frames: MotionFrame[] = [];

    let segmentNames: string[] = [];
    let readingSegmentData = false;
    let headers: string[] = [];

    for (const line of lines) {
        const trimmedLine = line.trim();

        if (trimmedLine.startsWith('#') || trimmedLine === '') {
            continue;
        }

        if (trimmedLine.startsWith('[')) {
            if (trimmedLine === '[SegmentNames&Hierarchy]') {
                continue;
            }
            if (trimmedLine === '[SegmentData]') {
                readingSegmentData = true;
                continue;
            }
            continue;
        }

        if (!readingSegmentData) {
            if (trimmedLine.startsWith('Frame#')) {
                headers = trimmedLine.split(',').map(h => h.trim());
                continue;
            }
            if (trimmedLine === 'Segment,Parent') {
                continue;
            }
            const parts = trimmedLine.split(',');
            if (parts.length === 2) {
                segmentNames.push(parts[0].trim());
            }
            continue;
        }

        if (readingSegmentData && headers.length > 0) {
            const values = trimmedLine.split(',').map(v => v.trim());
            if (values.length < 2 || !values[0]) continue;

            const frameIndex = parseInt(values[0]);
            if (isNaN(frameIndex)) continue;

            const boneMap = new Map<string, { position: THREE.Vector3; rotation: THREE.Quaternion }>();

            for (let i = 0; i < segmentNames.length; i++) {
                const baseIndex = 2 + i * 16;
                if (baseIndex + 7 < values.length) {
                    const x = parseFloat(values[baseIndex + 8]) || 0;
                    const y = parseFloat(values[baseIndex + 9]) || 0;
                    const z = parseFloat(values[baseIndex + 10]) || 0;
                    const qx = parseFloat(values[baseIndex + 11]) || 0;
                    const qy = parseFloat(values[baseIndex + 12]) || 0;
                    const qz = parseFloat(values[baseIndex + 13]) || 0;
                    const qw = parseFloat(values[baseIndex + 14]) || 1;

                    boneMap.set(segmentNames[i], {
                        position: new THREE.Vector3(x, y, z),
                        rotation: new THREE.Quaternion(qx, qy, qz, qw),
                    });
                }
            }

            frames.push({ bones: boneMap });
        }
    }

    return frames;
};

const updateSkeleton = (frame: MotionFrame) => {
    if (!skeleton) return;

    skeleton.children.forEach(child => {
        if (child instanceof THREE.Mesh) {
            const boneName = child.name;
            if (boneName && frame.bones.has(boneName)) {
                const boneData = frame.bones.get(boneName)!;
                child.position.copy(boneData.position);
                child.position.y = -child.position.y + 1000;
                child.position.z = -child.position.z;
                child.quaternion.copy(boneData.rotation);
            }
        }

        if (child instanceof THREE.Line) {
            const [parentName, childName] = child.name.split('-');
            if (parentName && childName && frame.bones.has(parentName) && frame.bones.has(childName)) {
                const parentPos = frame.bones.get(parentName)!.position.clone();
                const childPos = frame.bones.get(childName)!.position.clone();

                parentPos.y = -parentPos.y + 1000;
                parentPos.z = -parentPos.z;
                childPos.y = -childPos.y + 1000;
                childPos.z = -childPos.z;

                const positions = child.geometry.attributes.position.array as Float32Array;
                positions[0] = parentPos.x;
                positions[1] = parentPos.y;
                positions[2] = parentPos.z;
                positions[3] = childPos.x;
                positions[4] = childPos.y;
                positions[5] = childPos.z;
                child.geometry.attributes.position.needsUpdate = true;
            }
        }
    });
};

const animate = () => {
    animationId = requestAnimationFrame(animate);

    if (isPlaying && motionData.length > 0) {
        currentFrame = (currentFrame + 1) % motionData.length;
        updateSkeleton(motionData[currentFrame]);
    }

    controls.update();
    renderer.render(scene, camera);
};

const loadMotionData = async (url: string) => {
    try {
        const response = await fetch(url);
        const text = await response.text();
        motionData = parseCSV(text);
        currentFrame = 0;
        if (motionData.length > 0) {
            updateSkeleton(motionData[0]);
        }
    } catch (error) {
        console.error('Failed to load motion data:', error);
    }
};

const play = () => {
    isPlaying = true;
};

const pause = () => {
    isPlaying = false;
};

const reset = () => {
    currentFrame = 0;
    if (motionData.length > 0) {
        updateSkeleton(motionData[0]);
    }
};

const handleResize = () => {
    if (!containerRef.value) return;

    camera.aspect = containerRef.value.clientWidth / containerRef.value.clientHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(containerRef.value.clientWidth, containerRef.value.clientHeight);
};

onMounted(() => {
    initScene();
    animate();
    window.addEventListener('resize', handleResize);

    if (props.csvUrl) {
        loadMotionData(props.csvUrl);
    }
});

onUnmounted(() => {
    window.removeEventListener('resize', handleResize);
    if (animationId) {
        cancelAnimationFrame(animationId);
    }
    if (renderer) {
        renderer.dispose();
    }
});

watch(() => props.csvUrl, (newUrl) => {
    if (newUrl) {
        loadMotionData(newUrl);
    }
});

defineExpose({
    play,
    pause,
    reset,
});
</script>

<style scoped>
.motion-3d-viewer {
    width: 100%;
    height: 100%;
    min-height: 300px;
    border-radius: 8px;
    overflow: hidden;
}
</style>
