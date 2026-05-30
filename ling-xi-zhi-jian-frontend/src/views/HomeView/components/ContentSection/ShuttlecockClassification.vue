<script setup lang="ts"></script>

<template>
  <div class="shuttlecock-classification">
    <h2>毽球的分类</h2>
    <p>在体育总局社体中心的推动下，如今的毽球运动可以大致分为以下两类：</p>
    <ul>
      <li>花毽：属于竞技性比赛，注重技巧和花式动作。</li>
      <li>网毽（毽球）：属于对抗性比赛，具有很强的观赏性。它结合了：
        <ul>
          <li>羽毛球的场地</li>
          <li>排球的规则</li>
          <li>足球的技术</li>
        </ul>
        <p>比赛中，双方隔网进攻和防守，决出胜负。对抗形式包括单人、双人和团体等。</p>
      </li>
    </ul>
    <p class="source">出处： <a href="https://sports.cctv.com/2024/11/24/VIDERpVv0lbUx7RZKGrRUWMv241124.shtml" target="_blank" class="source-link">《中国少数民族传统体育集萃》 第六集：毽球</a></p>
  </div>
</template>

<style scoped lang="scss">
.shuttlecock-classification {
  padding: 20px;
  h2 {
    font-size: 1.5rem;
    margin-bottom: 15px;
    color: #333;
  }
  p {
    font-size: 1rem;
    line-height: 1.6;
    color: #666;
    margin-bottom: 15px;
  }
  ul {
    margin-bottom: 15px;
    padding-left: 20px;
    li {
      margin-bottom: 10px;
      color: #666;
      ul {
        margin-top: 5px;
        margin-bottom: 10px;
        li {
          margin-bottom: 5px;
        }
      }
    }
  }
  .source {
    font-style: italic;
    font-size: 0.9rem;
    color: #999;
  }
  .source-link {
    color: #659ee8;
    text-decoration: none;
    &:hover {
      text-decoration: underline;
    }
  }
}
</style>