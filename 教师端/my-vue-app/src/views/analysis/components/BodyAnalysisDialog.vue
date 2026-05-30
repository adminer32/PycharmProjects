<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { analysisApi } from '@/api/analysisApi'

const props = defineProps<{
  visible: boolean
  studentId: number | null
  studentName?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const loading = ref(false)
const physiqueData = ref<any>(null)

const fmt = (val: any, decimals = 1): string => {
  if (val === null || val === undefined) return '0.0'
  const n = Number(val)
  return isNaN(n) ? '0.0' : n.toFixed(decimals)
}

const hasData = computed(() => physiqueData.value && physiqueData.value.id)

const bodyData = computed(() => {
  const p = physiqueData.value
  if (!p) return null

  const getVal = (field: any): number => {
    if (field === null || field === undefined) return 0
    return Number(field) || 0
  }

  const muscleTotal = getVal(p.segmentSkeletalMuscleTotal)
  const fatTotal = getVal(p.segmentFatTotal)

  let bodyTypeColor = '#10b981'
  const bt = (p.bodyType || '标准型').toString()
  if (bt.includes('偏瘦') && bt.includes('肌肉')) bodyTypeColor = '#3b82f6'
  else if (bt.includes('强壮')) bodyTypeColor = '#8b5cf6'
  else if (bt.includes('偏胖')) bodyTypeColor = '#f59e0b'
  else if (bt.includes('偏瘦') && !bt.includes('肌肉')) bodyTypeColor = '#0ea5e9'

  return {
    skeletalMuscle: {
      total: fmt(muscleTotal),
      trunk: fmt(p.segmentSkeletalTrunk),
      rightArm: fmt(p.segmentSkeletalRightUpper),
      leftArm: fmt(p.segmentSkeletalLeftUpper),
      rightLeg: fmt(p.segmentSkeletalRightLower),
      leftLeg: fmt(p.segmentSkeletalLeftLower)
    },
    bodyFat: {
      total: fmt(fatTotal),
      trunk: fmt(p.segmentFatTrunk),
      rightArm: fmt(p.segmentFatRightUpper),
      leftArm: fmt(p.segmentFatLeftUpper),
      rightLeg: fmt(p.segmentFatRightLower),
      leftLeg: fmt(p.segmentFatLeftLower)
    },
    bodyType: p.bodyType || '标准型',
    bodyTypeColor,
    bodyShape: p.bodyShape || '匀称型',
    bodyAge: p.bodyAge ?? '-',
    heartRate: p.heartRate ?? '-',
    weight: p.weight ? fmt(p.weight, 1) : null,
    bmi: p.bmi ? fmt(p.bmi, 1) : null,
    fatPercentage: p.fatPercentage ? fmt(p.fatPercentage, 1) + '%' : null,
    bmiValue: getVal(p.bmi),
    fatPercentValue: getVal(p.fatPercentage)
  }
})

const fetchPhysiqueData = async () => {
  if (!props.studentId) return

  loading.value = true
  try {
    const res = await analysisApi.getStudentPhysique(props.studentId)
    if (res.data && res.data.id) {
      physiqueData.value = res.data
    } else {
      physiqueData.value = null
    }
  } catch (error) {
    console.log('未找到体质数据')
    physiqueData.value = null
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    fetchPhysiqueData()
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="visible" class="body-analysis-overlay" @click.self="emit('update:visible', false)">
        <Transition name="dialog-scale">
          <div v-if="visible" class="body-analysis-dialog">
            <div class="dialog-header">
              <div class="header-left">
                <h3>📊 运动分析详情</h3>
                <span v-if="studentName" class="student-name">{{ studentName }}</span>
              </div>
              <button class="close-btn" @click="emit('update:visible', false)">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>

            <div class="dialog-content">
              <div v-if="loading" class="loading-state">
                <div class="loading-spinner"></div>
                <span>加载中...</span>
              </div>

              <template v-else>
                <div v-if="!hasData" class="no-data-state">
                  <div class="no-data-icon">📭</div>
                  <h4>暂无体质数据</h4>
                  <p>该学生尚未录入体质数据</p>
                </div>

                <template v-else>
                  <!-- 身体数据分布 -->
                  <div class="body-data-section" v-if="bodyData">
                    <h4 class="section-title">身体数据分布</h4>
                    
                    <!-- 骨骼肌 -->
                    <div class="data-category muscle">
                      <div class="category-header">
                        <span class="category-name">骨骼肌</span>
                        <span class="category-total">{{ bodyData.skeletalMuscle.total }}kg</span>
                      </div>
                      <div class="data-grid">
                        <div class="data-item">
                          <span class="data-label">躯干</span>
                          <span class="data-value muscle">{{ bodyData.skeletalMuscle.trunk }}kg</span>
                        </div>
                        <div class="data-item">
                          <span class="data-label">右上肢</span>
                          <span class="data-value muscle">{{ bodyData.skeletalMuscle.rightArm }}kg</span>
                        </div>
                        <div class="data-item">
                          <span class="data-label">左上肢</span>
                          <span class="data-value muscle">{{ bodyData.skeletalMuscle.leftArm }}kg</span>
                        </div>
                        <div class="data-item">
                          <span class="data-label">右下肢</span>
                          <span class="data-value muscle">{{ bodyData.skeletalMuscle.rightLeg }}kg</span>
                        </div>
                        <div class="data-item">
                          <span class="data-label">左下肢</span>
                          <span class="data-value muscle">{{ bodyData.skeletalMuscle.leftLeg }}kg</span>
                        </div>
                      </div>
                    </div>

                    <!-- 脂肪 -->
                    <div class="data-category fat">
                      <div class="category-header">
                        <span class="category-name">脂肪</span>
                        <span class="category-total">{{ bodyData.bodyFat.total }}kg</span>
                      </div>
                      <div class="data-grid">
                        <div class="data-item">
                          <span class="data-label">躯干</span>
                          <span class="data-value fat">{{ bodyData.bodyFat.trunk }}kg</span>
                        </div>
                        <div class="data-item">
                          <span class="data-label">右上肢</span>
                          <span class="data-value fat">{{ bodyData.bodyFat.rightArm }}kg</span>
                        </div>
                        <div class="data-item">
                          <span class="data-label">左上肢</span>
                          <span class="data-value fat">{{ bodyData.bodyFat.leftArm }}kg</span>
                        </div>
                        <div class="data-item">
                          <span class="data-label">右下肢</span>
                          <span class="data-value fat">{{ bodyData.bodyFat.rightLeg }}kg</span>
                        </div>
                        <div class="data-item">
                          <span class="data-label">左下肢</span>
                          <span class="data-value fat">{{ bodyData.bodyFat.leftLeg }}kg</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 底部统计卡片 -->
                  <div class="stats-cards" v-if="bodyData">
                    <div class="stat-card">
                      <div class="stat-label">身体类型</div>
                      <div class="stat-value" :style="{ color: bodyData.bodyTypeColor }">
                        {{ bodyData.bodyType }}
                      </div>
                    </div>
                    <div class="stat-card">
                      <div class="stat-label">身体形态</div>
                      <div class="stat-value">{{ bodyData.bodyShape }}</div>
                    </div>
                    <div class="stat-card">
                      <div class="stat-label">身体年龄</div>
                      <div class="stat-value">{{ bodyData.bodyAge }}岁</div>
                    </div>
                    <div class="stat-card">
                      <div class="stat-label">心率</div>
                      <div class="stat-value">{{ bodyData.heartRate }}bpm</div>
                    </div>
                  </div>
                </template>
              </template>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script lang="ts">
function getScoreClass(score: number): string {
  if (score >= 80) return 'excellent'
  if (score >= 60) return 'good'
  if (score >= 40) return 'fair'
  return 'poor'
}
</script>

<style scoped>
.body-analysis-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.body-analysis-dialog {
  width: 90%;
  max-width: 650px;
  max-height: 85vh;
  background: white;
  border-radius: 20px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dialog-header h3 {
  font-size: 1.125rem;
  font-weight: 700;
  margin: 0;
  color: #1e293b;
}

.student-name {
  background: #0ea5e9;
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 500;
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  color: #64748b;
}

.close-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

.dialog-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #64748b;
  gap: 16px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e2e8f0;
  border-top-color: #0ea5e9;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.no-data-state {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.no-data-icon {
  font-size: 4rem;
  margin-bottom: 16px;
}

.no-data-state h4 {
  font-size: 1.125rem;
  color: #64748b;
  margin: 0 0 8px;
}

.no-data-state p {
  font-size: 0.875rem;
  margin: 0;
}

.motion-info-section {
  display: flex;
  justify-content: space-between;
  background: #f8fafc;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 0.75rem;
  color: #94a3b8;
  font-weight: 500;
}

.info-value {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1e293b;
}

.info-value.highlight {
  color: #0ea5e9;
}

.info-value.mono {
  font-family: monospace;
  font-size: 0.8rem;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.section-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px;
}

.body-data-section {
  margin-bottom: 20px;
}

.data-category {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}

.data-category.muscle {
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
  border-color: #ddd6fe;
}

.data-category.fat {
  background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
  border-color: #fed7aa;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.category-name {
  font-size: 0.85rem;
  font-weight: 600;
}

.data-category.muscle .category-name {
  color: #7c3aed;
}

.data-category.fat .category-name {
  color: #ea580c;
}

.category-total {
  font-size: 0.9rem;
  font-weight: 700;
}

.data-category.muscle .category-total {
  color: #7c3aed;
}

.data-category.fat .category-total {
  color: #ea580c;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.data-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.data-label {
  font-size: 0.8rem;
  color: #64748b;
}

.data-value {
  font-size: 0.9rem;
  font-weight: 600;
}

.data-value.muscle {
  color: #7c3aed;
}

.data-value.fat {
  color: #ea580c;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  background: #f8fafc;
  border-radius: 12px;
  padding: 12px;
  text-align: center;
}

.stat-label {
  font-size: 0.7rem;
  color: #94a3b8;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 0.9rem;
  font-weight: 700;
  color: #1e293b;
}

.scores-section {
  margin-bottom: 20px;
}

.score-summary {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
  border-radius: 16px;
  padding: 20px;
}

.main-score {
  text-align: center;
  margin-bottom: 16px;
}

.main-score .score-label {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 8px;
}

.main-score .score-value {
  font-size: 3rem;
  font-weight: 700;
  line-height: 1;
}

.main-score .score-value.excellent { color: #10b981; }
.main-score .score-value.good { color: #3b82f6; }
.main-score .score-value.fair { color: #f59e0b; }
.main-score .score-value.poor { color: #ef4444; }

.main-score .score-unit {
  font-size: 0.875rem;
  color: #94a3b8;
  margin-top: 4px;
}

.sub-scores {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.sub-score {
  background: white;
  border-radius: 12px;
  padding: 12px;
  text-align: center;
}

.sub-label {
  font-size: 0.75rem;
  color: #64748b;
  display: block;
  margin-bottom: 6px;
}

.sub-value {
  font-size: 1.25rem;
  font-weight: 700;
}

.sub-value.excellent { color: #10b981; }
.sub-value.good { color: #3b82f6; }
.sub-value.fair { color: #f59e0b; }
.sub-value.poor { color: #ef4444; }

.advice-section {
  margin-bottom: 16px;
}

.advice-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.advice-item {
  display: flex;
  gap: 12px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
}

.advice-icon {
  font-size: 1.25rem;
  flex-shrink: 0;
}

.advice-content {
  flex: 1;
  min-width: 0;
}

.advice-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 4px;
}

.advice-text {
  font-size: 0.85rem;
  color: #64748b;
  line-height: 1.5;
  word-wrap: break-word;
}

.dialog-enter-active,
.dialog-leave-active {
  transition: opacity 0.3s ease;
}

.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}

.dialog-scale-enter-active,
.dialog-scale-leave-active {
  transition: transform 0.3s ease;
}

.dialog-scale-enter-from,
.dialog-scale-leave-to {
  transform: scale(0.95);
}

@media (max-width: 600px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .advice-grid {
    grid-template-columns: 1fr;
  }
  
  .sub-scores {
    grid-template-columns: 1fr;
  }
  
  .info-row {
    align-items: center;
  }
}
</style>
