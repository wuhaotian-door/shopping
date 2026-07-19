<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  document.title = '智能问答 — 码上非遗'
})
</script>

<template>
  <div class="rag-page">
    <div class="rag-hero">
      <div class="rag-icon">🤖</div>
      <h1>非遗 AI 智能问答</h1>
      <p class="rag-subtitle">基于 RAG 技术的非遗知识智能助手</p>
    </div>

    <div class="rag-placeholder">
      <div class="placeholder-card">
        <div class="ph-icon">🚧</div>
        <h3>功能开发中</h3>
        <p>此模块预留用于集成 RAG（检索增强生成）AI 对话系统</p>
        <ul class="feature-list">
          <li>📚 非遗知识库检索 — 查询非遗技艺、传承人、项目等</li>
          <li>💬 AI 智能对话 — 基于大语言模型的自然语言问答</li>
          <li>🔍 语义搜索 — 理解用户意图精准匹配非遗内容</li>
          <li>📖 知识图谱 — 非遗项目、传承人、地区关联展示</li>
          <li>🎯 个性化推荐 — 根据用户兴趣推荐非遗内容</li>
        </ul>
      </div>

      <div class="tech-stack">
        <h4>建议技术栈</h4>
        <div class="stack-tags">
          <span class="stack-tag">LangChain</span>
          <span class="stack-tag">OpenAI / 本地大模型</span>
          <span class="stack-tag">ChromaDB / Pinecone</span>
          <span class="stack-tag">FastAPI / Flask</span>
          <span class="stack-tag">Vue 3</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rag-page { min-height: calc(100vh - 64px); background: #fcf4e8; }

.rag-hero {
  text-align: center; padding: 60px 20px 40px;
  background: linear-gradient(180deg, #fff 0%, #fcf4e8 100%);
}
.rag-icon { font-size: 72px; margin-bottom: 16px; }
.rag-hero h1 { font-size: 32px; color: #c41e3a; margin: 0 0 8px; }
.rag-subtitle { font-size: 16px; color: #999; margin: 0; }

.rag-placeholder { max-width: 800px; margin: 0 auto; padding: 0 20px 60px; }
.placeholder-card {
  background: #fff; border-radius: 16px; padding: 40px; text-align: center;
  box-shadow: 0 2px 16px rgba(196,30,58,0.06); margin-bottom: 24px;
}
.ph-icon { font-size: 56px; margin-bottom: 12px; }
.placeholder-card h3 { font-size: 22px; color: #c41e3a; margin: 0 0 8px; }
.placeholder-card > p { color: #999; font-size: 14px; margin: 0 0 24px; }
.feature-list { text-align: left; list-style: none; padding: 0; max-width: 500px; margin: 0 auto; }
.feature-list li { padding: 10px 0; border-bottom: 1px solid #ffe9ec; font-size: 14px; color: #555; }
.feature-list li:last-child { border-bottom: none; }

.tech-stack { text-align: center; }
.tech-stack h4 { font-size: 14px; color: #999; margin: 0 0 12px; }
.stack-tags { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.stack-tag {
  padding: 6px 16px; font-size: 13px; color: #c41e3a; background: #fff5f7;
  border: 1px solid #ffe9ec; border-radius: 20px;
}
</style>
