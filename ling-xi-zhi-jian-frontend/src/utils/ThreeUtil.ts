import * as THREE from 'three';
import { Camera, PerspectiveCamera } from 'three';
import type { Font } from 'three/examples/jsm/loaders/FontLoader.js';
import { FontLoader } from 'three/examples/jsm/loaders/FontLoader.js';
import type { GLTF } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { FBXLoader } from 'three/examples/jsm/loaders/FBXLoader.js';

export class ThreeUtil {
    public static loadFont = (
        jsonFilePath: string,
        onProgress?: (xhr: { total: number; loaded: number }) => void,
    ): Promise<Font> => {
        return new Promise<Font>((resolve, reject) => {
            new FontLoader().load(
                jsonFilePath,
                (font: Font) => resolve(font),
                onProgress,
                (err: unknown) => reject(err),
            );
        });
    };

    public static loadGlb = (
        glb: string,
        onProgress?: (xhr: { total: number; loaded: number }) => void,
    ) => {
        return new Promise<GLTF>((resolve, reject) => {
            new GLTFLoader().load(
                glb,
                (gltf: GLTF) => resolve(gltf),
                onProgress,
                (err: unknown) => reject(err),
            );
        });
    };
    public static loadFBX = (
        fbx: string,
        onProgress?: (xhr: { total: number; loaded: number }) => void,
    ) => {
        return new Promise<THREE.Group<THREE.Object3DEventMap>>(
            (resolve, reject) => {
                new FBXLoader().load(
                    fbx, // FBX 文件的路径
                    (data: THREE.Group<THREE.Object3DEventMap>) => {
                        resolve(data);
                    },
                    onProgress,
                    (err: unknown) => reject(err),
                );
            },
        );
    };

    public static createControls = (
        camera: Camera,
        canvas: HTMLCanvasElement,
    ) => {
        const controls = new OrbitControls(camera, canvas);
        controls.enableDamping = true; // 使动画循环使用时阻尼或自转 意思是否有惯性
        controls.dampingFactor = 0.95; // 阻尼系数，越大则惯性越大
        controls.screenSpacePanning = false; // 右键拖拽时是否在屏幕空间中
        controls.minDistance = 300; // 相机距离原点的最小距离
        controls.maxDistance = 800; // 相机距离原点的最大距离
        controls.maxPolarAngle = Math.PI * 0.45; // 相机距离原点的最大角度

        // 关闭移动
        controls.enablePan = false;
        return controls;
    };

    public static createCamera = (
        canvas: HTMLCanvasElement,
    ): PerspectiveCamera => {
        return new THREE.PerspectiveCamera(
            70,
            canvas.offsetWidth / canvas.offsetHeight,
            1, // 最近
            10000, // 最远
        );
    };

    public static createWebGLRenderer = (canvas: HTMLCanvasElement) => {
        return new THREE.WebGLRenderer({
            canvas: canvas,
            antialias: true, // 抗锯齿
            alpha: true, // 启用透明
            preserveDrawingBuffer: false, // 保持绘图缓冲区，后续可保存为图片
        });
    };
}
