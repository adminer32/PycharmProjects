import Mock from 'mockjs';

// 全局配置 Mock.js 的延迟
Mock.setup({
  timeout: '200-600'
});

// 模拟一些通用的后端接口
Mock.mock(/\/api\/system\/user\/login/, 'post', {
  code: 200,
  message: 'Success',
  data: 'mock-sa-token-1234567890'
});

// 你可以在这里添加更多特定业务逻辑的 Mock 接口
export default Mock;

