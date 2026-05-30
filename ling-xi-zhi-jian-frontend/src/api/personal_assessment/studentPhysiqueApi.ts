import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel.d';
import { useUserStore } from '@/stores/userStore';

export interface StudentPhysique {
    id?: number;
    student_id: number;
    weight: number;
    bmi: number;
    fat_percentage: number;
    skeletal_muscle_mass: number;
    visceral_fat_level: number;
    limb_skeletal_muscle_index: number;
    estimated_waist_hip_ratio: number;
    body_type: string;
    body_shape: string;
    basal_metabolism_rate: number;
    moisture_rate: number;
    bone_salt_amount: number;
    protein_percentage: number;
    lean_body_mass: number;
    body_age: number;
    heart_rate: number;
    segment_moisture: number;
    segment_protein: number;
    segment_fat_mass: number;
    segment_bone_salt: number;
    segment_fat_total: number;
    segment_fat_right_upper: number;
    segment_fat_left_upper: number;
    segment_fat_trunk: number;
    segment_fat_right_lower: number;
    segment_fat_left_lower: number;
    segment_skeletal_muscle_total: number;
    segment_skeletal_right_upper: number;
    segment_skeletal_left_upper: number;
    segment_skeletal_trunk: number;
    segment_skeletal_right_lower: number;
    segment_skeletal_left_lower: number;
    create_time?: string;
    update_time?: string;
}

export const getStudentPhysiqueApi = async (): Promise<Result<StudentPhysique>> => {
    const userStore = useUserStore();
    const studentId = Number(userStore.myInfo.userId);
    return HttpUtil.get('/api/physique/query', { params: { student_id: studentId } });
};

export const createStudentPhysiqueApi = async (data: Partial<StudentPhysique>): Promise<Result<null>> => {
    return HttpUtil.post('/api/physique/create', data);
};

export const updateStudentPhysiqueApi = async (data: Partial<StudentPhysique>): Promise<Result<null>> => {
    return HttpUtil.put('/api/physique/update', data);
};
