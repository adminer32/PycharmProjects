<template>
    <div class="questions-and-answers">
        <div class="question" v-slide-in>
            <div class="content">毽球基本动作分为哪几大类？</div>
            <img
                class="avatar"
                src="../../../assets/home_view/用户默认头像.png"
                alt=""
            />
        </div>

        <div class="answer" v-slide-in>
            <AiAvatar />
            <div class="answer-content">
                <div class="text">
                    <h1>毽球基本动作分类</h1>
                    <div class="category">
                        <h2>1. 准备姿势</h2>
                        <ul>
                            <li>
                                <strong>左右开位站立</strong>
                                <p class="description">
                                    双脚左右分开，与肩同宽或略宽，重心稍前移，后脚跟可稍抬起，保持身体稳定并随时准备移动。
                                </p>
                            </li>
                            <li>
                                <strong>前后开位站立</strong>
                                <p class="description">
                                    双脚前后分开，前脚脚尖向前，后脚脚尖稍内扣，重心放在两脚之间，便于前后移动。
                                </p>
                            </li>
                        </ul>
                    </div>

                    <div class="category">
                        <h2>2. 步法移动</h2>
                        <ul>
                            <li>
                                <strong>前上步</strong
                                >：向前迈出一步，身体重心随之向前移动，用于快速接近前方来球。
                            </li>
                            <li>
                                <strong>后撤步</strong
                                >：向后撤一步，身体重心随之向后移动，用于调整位置，准备接应后方来球。
                            </li>
                            <li>
                                <strong>滑步</strong
                                >：双脚平行移动，身体重心保持稳定，用于快速横向移动，调整位置。
                            </li>
                            <li>
                                <strong>交叉步</strong
                                >：一腿交叉于另一腿前方或后方，快速改变方向，用于灵活调整位置。
                            </li>
                            <li>
                                <strong>并步</strong
                                >：双脚并拢移动，用于快速调整位置，尤其在近距离移动时较为有效。
                            </li>
                            <li>
                                <strong>跨步</strong
                                >：迈大步跨越一定距离，用于快速调整位置，准备接应远处来球。
                            </li>
                            <li>
                                <strong>转体上步</strong
                                >：身体转动并迈出一步，用于调整方向和位置，准备击球。
                            </li>
                            <li>
                                <strong>跑动步</strong
                                >：快速奔跑，用于快速移动到指定位置，尤其在追击远距离来球时使用。
                            </li>
                        </ul>
                    </div>

                    <div class="category">
                        <h2>3. 踢球动作</h2>
                        <ul>
                            <li>
                                <strong>脚内侧踢球</strong>
                                <p class="description">
                                    抬大腿，小腿向内甩踢，用脚内侧的足弓部位击球，适合传球和短距离击球。
                                </p>
                            </li>
                            <li>
                                <strong>脚外侧踢球</strong>
                                <p class="description">
                                    侧身，小腿向外甩踢，用脚外侧部位击球，适合变向和长距离击球。
                                </p>
                            </li>
                            <li>
                                <strong>脚背踢球</strong>
                                <p class="description">
                                    绷脚尖，抖动脚腕发力，用正脚背击球，适合大力击球和进攻。
                                </p>
                            </li>
                        </ul>
                    </div>

                    <div class="category">
                        <h2>4. 触球动作</h2>
                        <ul>
                            <li>
                                <strong>单腿触球</strong>
                                <p class="description">
                                    抬起一条腿，用大腿正面前段击球，适合调整球的方向和高度。
                                </p>
                            </li>
                            <li>
                                <strong>双腿触球</strong>
                                <p class="description">
                                    双腿同时击球，用于控制球的方向和力度。
                                </p>
                            </li>
                            <li>
                                <strong>单腿触踢球</strong>
                                <p class="description">
                                    抬起一条腿，用脚部击球，适合进攻和防守。
                                </p>
                            </li>
                            <li>
                                <strong>腹部触踢球</strong>
                                <p class="description">
                                    用腹部击球，适合调整球的方向和高度。
                                </p>
                            </li>
                            <li>
                                <strong>胸部触踢球</strong>
                                <p class="description">
                                    用胸部击球，适合调整球的方向和高度。
                                </p>
                            </li>
                            <li>
                                <strong>头部触踢球</strong>
                                <p class="description">
                                    用头部击球，适合调整球的方向和高度。
                                </p>
                            </li>
                            <li>
                                <strong>头球</strong>
                                <p class="description">
                                    用头的正面或侧面击球，也可用头发甩动击球，适合进攻和防守。
                                </p>
                            </li>
                            <li>
                                <strong>倒钩球</strong>
                                <p class="description">
                                    背向或侧向球网，先抬大腿后甩小腿加速发力，绷脚尖击球，适合进攻。
                                </p>
                            </li>
                            <li>
                                <strong>脚踏球</strong>
                                <p class="description">
                                    向上抬腿后向下发力，用前脚掌推压击球，适合控制球的方向和力度。
                                </p>
                            </li>
                        </ul>
                    </div>

                    <div class="category">
                        <h2>5. 发球动作</h2>
                        <ul>
                            <li>
                                <strong>脚内侧发球</strong>
                                <p class="description">
                                    抬大腿带小腿，用内足弓部位向前上方送髋推踢，特点是稳、准、破坏性强。
                                </p>
                            </li>
                            <li>
                                <strong>正脚背发球</strong>
                                <p class="description">
                                    绷脚尖，用正脚背向前上发力挑踢，特点是平、快、准。
                                </p>
                            </li>
                            <li>
                                <strong>脚外侧发球</strong>
                                <p class="description">
                                    稍侧身站位，用脚外侧发力扫踢，特点是快、狠、攻击力强。
                                </p>
                            </li>
                        </ul>
                    </div>
                </div>
                <div ref="chartRef" class="chart"></div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import * as echarts from 'echarts';
import AiAvatar from '@/components/数据展示/AiAvatar/AiAvatar.vue';

// 初始化图表
const chartRef = ref(null);
let chartInstance: echarts.ECharts | null = null;

const initChart = () => {
    chartInstance = echarts.init(chartRef.value);

    import('@/assets/course_center_view/shuttlecock_knowledge.json').then(
        (res) => {
            const data = res.default;

            // 配置项
            const option = {
                tooltip: {
                    trigger: 'item',
                    triggerOn: 'mousemove',
                },
                series: [
                    {
                        type: 'tree',
                        data: [data],
                        top: '1%',
                        left: '7%',
                        bottom: '1%',
                        right: '20%',
                        symbol: 'emptyCircle',
                        symbolSize: 10,
                        initialTreeDepth: 2,
                        orient: 'LR',
                        label: {
                            position: 'right',
                            fontSize: 12,
                        },
                        leaves: {
                            label: {
                                position: 'right',
                            },
                        },
                        expandAndCollapse: true,
                        animationDuration: 550,
                        animationDurationUpdate: 750,
                    },
                ],
            };

            chartInstance?.setOption(option);
        },
    );
};

onMounted(() => {
    initChart();
});
</script>

<style scoped lang="scss">
.questions-and-answers {
    height: auto;
    padding: 0 100px;

    .avatar,
    .ai-avatar {
        $size: 60px;
        height: $size;
        width: $size;
        border-radius: 50%;
        box-shadow: 1px 1px 30px #aaaaaa55;
    }

    .question {
        margin: 10px 20px;
        display: flex;
        flex-direction: row;
        justify-content: flex-end;

        .content {
            margin: 20px;
            position: relative;
            background-color: #5893ef;
            padding: 20px;
            border-radius: 20px;
            border-top-right-radius: 0;
            background-color: rgba(255, 255, 255);
            box-shadow: 1px 1px 30px #aaaaaa55;
            backdrop-filter: blur(20px);
        }
    }

    .answer {
        margin: 10px 20px;
        display: flex;
        flex-direction: row;

        .answer-content {
            margin: 20px;
            position: relative;
            padding: 20px;
            background-color: #fff;
            width: 65vw;
            border-radius: 0 20px 20px 20px;
            background-color: rgba(255, 255, 255);
            box-shadow: 1px 1px 30px #aaaaaa55;
            backdrop-filter: blur(20px);

            .text {
                font-size: 14px;
                line-height: 1.5;

                h1 {
                    font-size: 24px;
                    color: #333;
                    margin-bottom: 20px;
                    text-align: center;
                    padding-bottom: 10px;
                    border-bottom: 2px solid #eee;
                }

                .category {
                    margin-bottom: 25px;
                }

                h2 {
                    font-size: 18px;
                    color: #4a90e2;
                    margin-bottom: 10px;
                    padding-left: 10px;
                    border-left: 4px solid #4a90e2;
                }

                ul {
                    list-style-type: none;
                }

                li {
                    margin-bottom: 15px;
                    position: relative;
                    padding-left: 20px;

                    &::before {
                        content: '•';
                        position: absolute;
                        left: 0;
                        color: #4a90e2;
                        font-weight: bold;
                    }

                    strong {
                        font-weight: bold;
                        font-size: 16px;
                    }

                    .description {
                        font-size: 14px;
                        color: #666;
                        margin-top: 5px;
                        padding-left: 20px;
                    }
                }
            }

            .chart {
                margin-top: 20px;
                height: 300px;
                background-color: #f9f9f9;
                border-radius: 10px;
                padding: 10px;
                box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
            }
        }
    }
}
</style>
