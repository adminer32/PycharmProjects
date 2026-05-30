export const ACTIONS_6 = ['盘踢', '跳踢', '踏踢', '磕踢', '拐踢', '绷踢']
export const actionNamesList = ['绷踢', '外摆踢', '里合踢', '盘踢', '踏踢', '拐踢', '磕踢', '跳踢']
export const baseColors = ['#0ea5e9', '#f97316', '#84cc16', '#ec4899', '#8b5cf6', '#ef4444', '#14b8a6', '#f59e0b']

export const getPriorityClass = (priority: string) => ({
  high: 'high-priority',
  medium: 'medium-priority',
  low: 'low-priority'
}[priority] || '')

export const getPriorityLabel = (priority: string) => ({
  high: '高优先级',
  medium: '中优先级',
  low: '低优先级'
}[priority] || '')

export const aiSuggestions = [
  {
    priority: 'high',
    title: '外摆踢整体薄弱',
    desc: '班级外摆踢平均分仅65分，低于预期目标，建议：',
    items: [
      '增加外摆踢专项训练课时，每周至少2次专项练习',
      '推荐观看《外摆技巧慢动作拆解》视频并布置观后感',
      '对得分低于60分的3名学生进行个别辅导和动作纠正',
      '组织小组互助练习，提升整体水平'
    ]
  },
  {
    priority: 'medium',
    title: '打卡率待提升',
    desc: '3名学生打卡率低于60%，影响整体训练效果，建议：',
    items: [
      '发送提醒通知给家长，了解学生打卡困难原因',
      '设置打卡奖励机制，提高积极性',
      '与这些学生进行一对一沟通，了解困难所在'
    ]
  },
  {
    priority: 'low',
    title: '盘踢基础巩固',
    desc: '盘踢基础良好，可适当增加难度，建议：',
    items: [
      '引入组合动作训练，提升连贯性',
      '对优秀学生提供进阶训练方案'
    ]
  }
]

export const personalAiSuggestions = [
  {
    priority: 'high',
    title: '外摆踢专项提升',
    desc: '外摆踢得分60分，低于班级平均(65分)，建议：',
    items: [
      '每日增加3组侧腰拉伸训练，每组保持15秒',
      '观看《外摆技巧慢动作拆解》视频3次',
      '完成AI课程《外摆踢基础+前测》',
      '录制自己的动作与示范视频进行对比分析'
    ]
  },
  {
    priority: 'medium',
    title: '磕踢节奏训练',
    desc: '磕踢节奏稳定性待提升，建议：',
    items: [
      '配合节拍器进行磕踢练习，从60bpm开始逐步提升',
      '完成作业《磕踢组合动作》的补充练习',
      '使用AI对比功能分析节奏差异'
    ]
  },
  {
    priority: 'low',
    title: '盘踢膝盖角度优化',
    desc: '动作流畅度良好，细节可进一步优化：',
    items: [
      '增加深蹲练习，提升腿部力量',
      '录制视频与示范对比，观察膝盖角度差异'
    ]
  }
]
