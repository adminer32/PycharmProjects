<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import Header from '@/views/Header/Header.vue';
import MotionAnalysisView from '@/views/PersonalAnalysisView/MotionAnalysisView/MotionAnalysisView.vue';
import LearningAnalysisVIew from '@/views/PersonalAnalysisView/LearningAnalysisView/LearningAnalysisView.vue';
import PhysicalAnalysisView from '@/views/PersonalAnalysisView/PhysicalAnalysisView/PhysicalAnalysisView.vue';

const route = useRoute();
const activeTab = ref('learningAnalysis');

const tabs = [
    {
        id: 'learningAnalysis',
        name: '学情分析',
        icon: 'icon-book-open',
        description: '学习数据统计'
    },
    {
        id: 'motionAnalysis',
        name: '动作分析',
        icon: 'icon-dongzuo',
        description: '动作评估报告'
    },
    {
        id: 'physicalAnalysis',
        name: '体质分析',
        icon: 'icon-shoucang',
        description: '体能数据分析'
    }
];

const setActiveTab = (tabId: string) => {
    activeTab.value = tabId;
};

onMounted(() => {
    if (route.query.tab) {
        activeTab.value = route.query.tab as string;
    }
});
</script>

<template>
    <div class="analysis-page">
        <Header />
        <div class="main">
            <div class="sidebar">
                <div class="sidebar-decoration">
                    <div class="decoration-circle circle-1"></div>
                    <div class="decoration-circle circle-2"></div>
                </div>
                
                <div class="sidebar-header">
                    <div class="header-icon">
                        <span class="iconfont icon-chart"></span>
                    </div>
                    <div class="header-text">
                        <h3>个人分析</h3>
                        <p>Personal Analysis</p>
                    </div>
                </div>

                <div class="sidebar-menu">
                    <div 
                        v-for="(tab, index) in tabs" 
                        :key="tab.id"
                        class="menu-item"
                        :class="{ active: activeTab === tab.id }"
                        :style="{ '--delay': `${index * 0.1}s` }"
                        @click="setActiveTab(tab.id)"
                    >
                        <div class="menu-item-bg"></div>
                        <div class="menu-item-content">
                            <div class="menu-icon">
                                <span class="iconfont" :class="tab.icon"></span>
                                <div class="icon-glow"></div>
                            </div>
                            <div class="menu-info">
                                <span class="menu-text">{{ tab.name }}</span>
                                <span class="menu-desc">{{ tab.description }}</span>
                            </div>
                            <div class="active-indicator"></div>
                        </div>
                    </div>
                </div>

                <div class="sidebar-footer">
                    <div class="footer-decoration"></div>
                    <div class="footer-text">
                        <span>翎翼毽球</span>
                    </div>
                </div>
            </div>
            
            <div class="content">
                <LearningAnalysisVIew v-if="activeTab === 'learningAnalysis'" />
                <MotionAnalysisView v-else-if="activeTab === 'motionAnalysis'" />
                <PhysicalAnalysisView v-else-if="activeTab === 'physicalAnalysis'" />
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.analysis-page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    overflow: hidden;
    background: linear-gradient(135deg, #f0f7ff 0%, #e6f0ff 100%);

    .main {
        flex: 1;
        display: flex;
        flex-direction: row;
        overflow: hidden;

        .sidebar {
            width: 260px;
            background: rgba(255, 255, 255, 0.85);
            backdrop-filter: blur(20px);
            border-right: 1px solid rgba(24, 144, 255, 0.1);
            display: flex;
            flex-direction: column;
            position: relative;
            overflow: hidden;
            box-shadow: 4px 0 24px rgba(24, 144, 255, 0.08);

            .sidebar-decoration {
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                height: 200px;
                pointer-events: none;
                overflow: hidden;

                .decoration-circle {
                    position: absolute;
                    border-radius: 50%;
                    background: linear-gradient(135deg, rgba(24, 144, 255, 0.15) 0%, rgba(105, 192, 255, 0.08) 100%);
                    
                    &.circle-1 {
                        width: 180px;
                        height: 180px;
                        top: -60px;
                        right: -60px;
                    }
                    
                    &.circle-2 {
                        width: 120px;
                        height: 120px;
                        top: 80px;
                        left: -40px;
                    }
                }
            }

            .sidebar-header {
                display: flex;
                align-items: center;
                gap: 14px;
                padding: 28px 24px;
                position: relative;
                z-index: 1;

                .header-icon {
                    width: 48px;
                    height: 48px;
                    border-radius: 14px;
                    background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    box-shadow: 0 4px 16px rgba(24, 144, 255, 0.35);

                    .icon-chart {
                        font-size: 24px;
                        color: #fff;
                    }
                }

                .header-text {
                    h3 {
                        margin: 0;
                        font-size: 18px;
                        font-weight: 700;
                        color: #333;
                        letter-spacing: 1px;
                    }

                    p {
                        margin: 2px 0 0 0;
                        font-size: 11px;
                        color: #999;
                        letter-spacing: 0.5px;
                    }
                }
            }

            .sidebar-menu {
                flex: 1;
                padding: 16px 16px;
                display: flex;
                flex-direction: column;
                gap: 8px;
                position: relative;
                z-index: 1;

                .menu-item {
                    position: relative;
                    border-radius: 14px;
                    overflow: hidden;
                    cursor: pointer;
                    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                    animation: slideIn 0.4s ease backwards;
                    animation-delay: var(--delay);

                    @keyframes slideIn {
                        from {
                            opacity: 0;
                            transform: translateX(-20px);
                        }
                        to {
                            opacity: 1;
                            transform: translateX(0);
                        }
                    }

                    .menu-item-bg {
                        position: absolute;
                        inset: 0;
                        background: linear-gradient(135deg, rgba(24, 144, 255, 0.08) 0%, rgba(105, 192, 255, 0.04) 100%);
                        opacity: 0;
                        transition: opacity 0.3s ease;
                    }

                    &:hover {
                        transform: translateX(4px);
                        
                        .menu-item-bg {
                            opacity: 1;
                        }

                        .menu-icon .icon-glow {
                            opacity: 0.5;
                        }
                    }

                    &.active {
                        .menu-item-bg {
                            opacity: 1;
                            background: linear-gradient(135deg, rgba(24, 144, 255, 0.15) 0%, rgba(105, 192, 255, 0.08) 100%);
                        }

                        .menu-icon {
                            .iconfont {
                                color: #fff;
                            }
                            
                            .icon-glow {
                                opacity: 1;
                                background: rgba(255, 255, 255, 0.3);
                            }
                        }

                        .menu-info {
                            .menu-text {
                                color: #1890ff;
                                font-weight: 600;
                            }

                            .menu-desc {
                                color: rgba(24, 144, 255, 0.7);
                            }
                        }

                        .active-indicator {
                            width: 4px;
                            height: 70%;
                            background: linear-gradient(180deg, #1890ff, #69c0ff);
                            border-radius: 0 4px 4px 0;
                        }
                    }

                    .menu-item-content {
                        display: flex;
                        align-items: center;
                        gap: 14px;
                        padding: 16px 18px;
                        position: relative;

                        .menu-icon {
                            width: 44px;
                            height: 44px;
                            border-radius: 12px;
                            background: linear-gradient(135deg, rgba(24, 144, 255, 0.12) 0%, rgba(105, 192, 255, 0.08) 100%);
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            position: relative;
                            transition: all 0.3s ease;

                            .iconfont {
                                font-size: 20px;
                                color: #666;
                                transition: all 0.3s ease;
                                position: relative;
                                z-index: 1;
                            }

                            .icon-glow {
                                position: absolute;
                                inset: -2px;
                                border-radius: 14px;
                                background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                                opacity: 0;
                                filter: blur(8px);
                                transition: opacity 0.3s ease;
                            }
                        }

                        .menu-info {
                            display: flex;
                            flex-direction: column;
                            gap: 3px;

                            .menu-text {
                                font-size: 15px;
                                font-weight: 500;
                                color: #333;
                                transition: all 0.3s ease;
                            }

                            .menu-desc {
                                font-size: 11px;
                                color: #999;
                                transition: all 0.3s ease;
                            }
                        }

                        .active-indicator {
                            position: absolute;
                            left: 0;
                            top: 50%;
                            transform: translateY(-50%);
                            width: 0;
                            height: 0;
                            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                        }
                    }
                }
            }

            .sidebar-footer {
                padding: 20px 24px;
                position: relative;
                z-index: 1;

                .footer-decoration {
                    height: 1px;
                    background: linear-gradient(90deg, transparent, rgba(24, 144, 255, 0.2), transparent);
                    margin-bottom: 16px;
                }

                .footer-text {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 8px;

                    span {
                        font-size: 12px;
                        color: #999;
                        letter-spacing: 1px;
                    }
                }
            }
        }

        .content {
            flex: 1;
            height: 100%;
            overflow-y: auto;
        }
    }
}
</style>