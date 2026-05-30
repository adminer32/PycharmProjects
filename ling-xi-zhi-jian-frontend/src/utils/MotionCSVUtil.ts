import * as THREE from 'three';

export interface MotionFrame {
    bones: Map<string, { position: THREE.Vector3; rotation: THREE.Quaternion }>;
}

export const boneConnections = [
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

export const allBoneNames = [
    'Hips',
    'Spine',
    'Spine1',
    'Spine2',
    'Spine3',
    'LeftShoulder',
    'LeftArm',
    'LeftForeArm',
    'LeftHand',
    'RightShoulder',
    'RightArm',
    'RightForeArm',
    'RightHand',
    'Neck',
    'Head',
    'LeftUpLeg',
    'LeftLeg',
    'LeftFoot',
    'LeftToeBase',
    'RightUpLeg',
    'RightLeg',
    'RightFoot',
    'RightToeBase',
];

export const parseMotionCSV = (csvText: string): MotionFrame[] => {
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

export const loadMotionCSV = async (url: string): Promise<MotionFrame[]> => {
    try {
        const response = await fetch(url);
        const text = await response.text();
        return parseMotionCSV(text);
    } catch (error) {
        console.error('Failed to load motion CSV:', error);
        return [];
    }
};

export const motionCSVMapping: Record<string, string> = {
    '绷踢': '/motion_capture_data/绷踢.csv',
    '盘踢': '/motion_capture_data/盘踢.csv',
    '磕踢': '/motion_capture_data/磕踢.csv',
    '对踢': '/motion_capture_data/对踢.csv',
    '拐踢': '/motion_capture_data/拐踢.csv',
    '头触球': '/motion_capture_data/头触球.csv',
    '跳踢': '/motion_capture_data/跳踢.csv',
    '胸触球': '/motion_capture_data/胸触球.csv',
};
