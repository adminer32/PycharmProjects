<script setup lang="ts">
import { Trophy } from '@element-plus/icons-vue'
import BodyAnalysisDialog from './BodyAnalysisDialog.vue'

const props = defineProps<{
  student: { id: number, name: string, score: number, avatar?: string } | null
}>()

const showBodyAnalysis = ref(false)
</script>

<template>
  <div v-if="student" class="student-profile-card">
    <div class="profile-header">
      <div class="avatar-container">
        <div class="avatar-large">{{ student.name.charAt(0) }}</div>
        <div class="status-indicator"></div>
      </div>
      <div class="profile-info">
        <div class="name-row">
          <h2 class="student-fullname">{{ student.name }}</h2>
          <span class="level-badge">进阶级球员</span>
        </div>
        <p class="student-meta">学号: {{ student.id }} | 所在班级: 实验班 | 评估得分: <strong>{{ student.score }}</strong></p>
        <div class="student-badges">
          <span class="badge progression">
            <el-icon><Trophy /></el-icon>
            进步 +5%
          </span>
          <span class="badge error">外摆踢薄弱</span>
          <span class="badge success">盘踢达人</span>
          <button class="body-analysis-btn" @click="showBodyAnalysis = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
              <line x1="9" y1="9" x2="9.01" y2="9"/>
              <line x1="15" y1="9" x2="15.01" y2="9"/>
            </svg>
            详细身体分析
          </button>
        </div>
      </div>
    </div>

    <BodyAnalysisDialog 
      v-model:visible="showBodyAnalysis"
      :student-id="student.id"
      :student-name="student.name"
    />
  </div>
</template>

<style scoped>
.student-profile-card {
  background: white;
  padding: 24px;
  border-radius: 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 4px 15px -3px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
  transition: all 0.3s ease;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar-container {
  position: relative;
}

.avatar-large {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #0ea5e9 0%, #38bdf8 100%);
  color: white;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.8rem;
  font-weight: 800;
  box-shadow: 0 10px 15px -3px rgba(14, 165, 233, 0.3);
}

.status-indicator {
  position: absolute;
  bottom: -4px;
  right: -4px;
  width: 14px;
  height: 14px;
  background: #10b981;
  border: 3px solid white;
  border-radius: 50%;
}

.profile-info {
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.student-fullname {
  font-size: 1.4rem;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
}

.level-badge {
  font-size: 0.75rem;
  padding: 4px 10px;
  background: #f1f5f9;
  color: #64748b;
  border-radius: 12px;
  font-weight: 600;
}

.student-meta {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 12px;
}

.student-meta strong {
  color: #0ea5e9;
  font-size: 1.1rem;
}

.student-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.badge {
  font-size: 0.75rem;
  padding: 4px 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
}

.badge.progression { background: #fff7ed; color: #f97316; }
.badge.error { background: #fef2f2; color: #ef4444; }
.badge.success { background: #f0fdf4; color: #10b981; }

.body-analysis-btn {
  font-size: 0.75rem;
  padding: 6px 14px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: 600;
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  color: #0369a1;
  border: 1px solid #7dd3fc;
  cursor: pointer;
  transition: all 0.3s ease;
}

.body-analysis-btn:hover {
  background: linear-gradient(135deg, #bae6fd 0%, #7dd3fc 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.25);
}

.body-analysis-btn svg {
  width: 14px;
  height: 14px;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
  }
  .name-row { justify-content: center; }
  .student-badges { justify-content: center; }
}
</style>
