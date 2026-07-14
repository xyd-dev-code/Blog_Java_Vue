<template>
  <div class="notfound">
    <div class="nf-inner">
      <div class="nf-num">404</div>
      <h1>页面找不到了</h1>
      <p>可能走丢了，去别处看看吧</p>
      <el-button type="primary" @click="$router.push('/')">回首页</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.notfound {
  min-height: 70vh;
  display: grid;
  place-items: center;
  text-align: center;
}
.nf-num {
  font-family: var(--font-serif);
  font-size: 120px;
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-autumn-500));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-weight: 600;
  line-height: 1;
}
.nf-inner p { color: var(--c-ink-soft); margin-bottom: 24px; }
.nf-inner h1 { font-family: var(--font-serif); margin: 16px 0 8px; }
@media (max-width: 480px) {
  .nf-num { font-size: 80px; }
  .notfound { padding: 24px; }
}
</style>