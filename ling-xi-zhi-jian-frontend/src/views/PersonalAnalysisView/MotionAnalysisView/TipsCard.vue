<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
    actionContext?: {
        action_type?: string;
        hip_score?: number;
        knee_score?: number;
        ankle_score?: number;
        foot_height_score?: number;
        overall_score?: number;
        feedback?: string[];
    };
}>();

const actionTips = computed(() => {
    if (!props.actionContext?.action_type) {
        return null;
    }

    const tips: Record<string, { title: string; points: string[] }> = {
        '盘踢': {
            title: '盘踢技术要点',
            points: [
                '支撑腿微屈站稳',
                '踢毽腿膝关节放松',
                '踝关节发力轻踢毽',
                '注意节奏控制'
            ]
        },
        '磕踢': {
            title: '磕踢技术要点',
            points: [
                '膝盖抬起同臀高',
                '小腿自然下垂',
                '用膝关节内侧磕踢',
                '力量适中节奏稳'
            ]
        },
        '外摆踢': {
            title: '外摆踢技术要点',
            points: [
                '髋关节放松外展',
                '膝关节带动小腿',
                '画弧外摆至体侧',
                '注意身体平衡'
            ]
        },
        '里合踢': {
            title: '里合踢技术要点',
            points: [
                '膝关节先外展',
                '小腿跟随画弧',
                '经体前合拢踢毽',
                '动作连贯协调'
            ]
        }
    };

    return tips[props.actionContext.action_type] || {
        title: '通用训练建议',
        points: [
            '保持身体平衡',
            '动作放松自然',
            '注意呼吸节奏',
            '循序渐进练习'
        ]
    };
});

const scoreLevel = computed(() => {
    const score = props.actionContext?.overall_score || 0;
    if (score >= 85) return { text: '优秀', color: '#52c41a' };
    if (score >= 70) return { text: '良好', color: '#1890ff' };
    if (score >= 60) return { text: '及格', color: '#faad14' };
    return { text: '需改进', color: '#ff4d4f' };
});
</script>

<template>
    <div class="tips-card">
        <div class="card-header">
            <div class="header-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <path d="M12 16v-4M12 8h.01"/>
                </svg>
            </div>
            <span class="header-title">动作小贴士</span>
        </div>

        <div v-if="!actionContext?.action_type" class="no-data">
            <p>完成动作分析后显示训练建议</p>
        </div>

        <template v-else>
            <div class="action-info">
                <span class="action-name">{{ actionContext.action_type }}</span>
                <span 
                    class="action-level" 
                    :style="{ background: scoreLevel.color }"
                >
                    {{ scoreLevel.text }}
                </span>
            </div>

            <div class="tips-list">
                <h4>{{ actionTips?.title }}</h4>
                <ul>
                    <li v-for="(point, index) in actionTips?.points" :key="index">
                        <span class="check-icon">✓</span>
                        {{ point }}
                    </li>
                </ul>
            </div>

            <div v-if="actionContext.feedback?.length" class="feedback-section">
                <h4>改进建议</h4>
                <div class="feedback-tags">
                    <span 
                        v-for="(fb, index) in actionContext.feedback" 
                        :key="index"
                        :class="['feedback-tag', fb.includes('✓') ? 'good' : 'warn']"
                    >
                        {{ fb }}
                    </span>
                </div>
            </div>
        </template>
    </div>
</template>

<style scoped lang="scss">
.tips-card {
    background: white;
    border-radius: 12px;
    padding: 16px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    border: 1px solid #f0f0f0;
    height: 100%;
}

.card-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;

    .header-icon {
        width: 28px;
        height: 28px;
        border-radius: 8px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;

        svg {
            width: 16px;
            height: 16px;
        }
    }

    .header-title {
        font-size: 13px;
        font-weight: 600;
        color: #333;
    }
}

.no-data {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 150px;
    color: #999;
    font-size: 11px;
    text-align: center;
}

.action-info {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;

    .action-name {
        font-size: 14px;
        font-weight: 600;
        color: #333;
    }

    .action-level {
        padding: 3px 10px;
        border-radius: 10px;
        color: white;
        font-size: 11px;
        font-weight: 500;
    }
}

.tips-list {
    h4 {
        font-size: 12px;
        color: #666;
        margin-bottom: 8px;
        font-weight: 500;
    }

    ul {
        list-style: none;
        padding: 0;
        margin: 0;
    }

    li {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 6px 0;
        font-size: 12px;
        color: #444;
        border-bottom: 1px dashed #f0f0f0;

        &:last-child {
            border-bottom: none;
        }

        .check-icon {
            color: #52c41a;
            font-weight: bold;
        }
    }
}

.feedback-section {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;

    h4 {
        font-size: 12px;
        color: #666;
        margin-bottom: 8px;
        font-weight: 500;
    }

    .feedback-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
    }

    .feedback-tag {
        padding: 4px 10px;
        border-radius: 14px;
        font-size: 11px;

        &.good {
            background: rgba(82, 196, 26, 0.1);
            color: #52c41a;
            border: 1px solid #52c41a;
        }

        &.warn {
            background: rgba(250, 173, 20, 0.1);
            color: #faad14;
            border: 1px solid #faad14;
        }
    }
}
</style>
