"""
Level 3 RAGAS — RAG 管道自动评估

核心概念:
  用另一个 LLM 当裁判，自动给 RAG 系统打分，取代人工判断。

四个指标:
  Faithfulness       — 答案有几句是胡编的？（需要 contexts + answer）
  Answer Relevancy   — 答案跑题了吗？（需要 question + answer）
  Context Precision  — 检索到的文档里，垃圾占多少？（需要 question + contexts + ground_truth）
  Context Recall     — 该查到的内容，查到了多少？（需要 question + contexts + ground_truth）

原理:
  - Faithfulness: 把答案拆成一句句声明，逐一和检索到的文档比对，看每句话有没有依据
  - Answer Relevancy: 从答案反向生成几个问题，算反向问题和原问题的语义相似度
  - Context Precision: 逐条判断检索文档是否与问题相关，算"信噪比"
  - Context Recall: 逐条判断标准答案的信息是否在检索文档中出现，算"查全率"

工作流:
  测试数据 [{question, ground_truth}, ...]
    → 跑现有 RAG 管道 (rag.py) → 拿到 answer + contexts
    → 组装 RAGAS Dataset
    → 用 DeepSeek 当裁判逐条打分
    → 返回各指标均值 + 每条明细

依赖: pip install ragas datasets
"""

import os
from typing import Any, Dict, List

from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

load_dotenv()

# ============================================================
# 评估用 LLM — 和业务 LLM 分开，temperature=0 保证一致性
# ============================================================
judge_llm = ChatOpenAI(
    model="deepseek-chat",
    base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    temperature=0,
    max_tokens=256,
)

# ============================================================
# 内置测试数据集 — 基于本项目的非遗知识库
# ============================================================
DEFAULT_TEST_DATA = [
    {
        "question": "紫砂壶是什么？产自哪里？",
        "ground_truth": "紫砂壶是江苏宜兴特产的手工陶土茶壶，以紫砂泥为原料，起源于明代，用于泡茶。",
    },
    {
        "question": "景泰蓝的制作工艺有哪些步骤？",
        "ground_truth": "景泰蓝制作包括制胎、掐丝、点蓝、烧蓝、磨光、镀金等步骤，是一种铜胎掐丝珐琅工艺。",
    },
    {
        "question": "蜀绣有什么特点？",
        "ground_truth": "蜀绣是四川传统刺绣工艺，以针法严谨、色彩鲜艳著称，与苏绣、湘绣、粤绣并称四大名绣。",
    },
    {
        "question": "京剧的主要角色类型有哪些？",
        "ground_truth": "京剧角色分生旦净丑四大行当，表演手段包括唱念做打，是中国的国粹。",
    },
    {
        "question": "什么是非物质文化遗产？",
        "ground_truth": "非物质文化遗产指世代相传的传统文化表现形式，包括口头传统、表演艺术、手工艺等。",
    },
    {
        "question": "桃花坞木版年画产自哪里？有什么特点？",
        "ground_truth": "桃花坞木版年画产自苏州，以木版套印、色彩鲜艳、构图饱满为特点，是南方年画的代表。",
    },
]


# ============================================================
# 评估入口
# ============================================================
def evaluate_rag(
    test_questions: List[Dict[str, str]] = None,
    evaluate_retrieval: bool = True,
    evaluate_generation: bool = True,
) -> Dict[str, Any]:
    """对 RAG 管道执行 RAGAS 评估

    Args:
        test_questions:  [{"question": "...", "ground_truth": "..."}, ...]
                         传 None 用内置测试数据
        evaluate_retrieval: 是否评估检索 (Context Precision + Recall)
        evaluate_generation: 是否评估生成 (Faithfulness + Answer Relevancy)

    Returns:
        {"scores": {指标名: 分数}, "details": [...], "test_count": N}
    """
    questions = test_questions or DEFAULT_TEST_DATA

    # ---- ① 跑 RAG 管道，收集 answer + contexts ----
    from rag import get_rag_chain

    rag_chain, retriever = get_rag_chain()

    records = []
    for item in questions:
        q = item["question"]
        gt = item.get("ground_truth", "")

        docs = retriever.invoke(q)
        contexts = [d.page_content for d in docs]

        try:
            answer = rag_chain.invoke(q)
        except Exception:
            answer = "(生成失败)"

        records.append({
            "question": q,
            "answer": answer,
            "contexts": contexts,
            "ground_truth": gt,
        })

    # ---- ② 组装 RAGAS Dataset ----
    try:
        from datasets import Dataset
    except ImportError:
        return {
            "error": "请安装: pip install datasets",
            "scores": None,
            "details": records,
        }

    dataset = Dataset.from_dict({
        "question": [r["question"] for r in records],
        "answer": [r["answer"] for r in records],
        "contexts": [r["contexts"] for r in records],
        "ground_truth": [r["ground_truth"] for r in records],
    })

    # ---- ③ 导入 RAGAS 并挑指标 ----
    try:
        from ragas import evaluate
        from ragas.llms import LangchainLLMWrapper
    except ImportError:
        return {
            "error": "请安装: pip install ragas",
            "scores": None,
            "details": records,
        }

    metrics = []

    if evaluate_generation:
        try:
            from ragas.metrics import faithfulness, answer_relevancy
        except ImportError:
            from ragas.metrics import Faithfulness, AnswerRelevancy
            faithfulness = Faithfulness()
            answer_relevancy = AnswerRelevancy()
        metrics.extend([faithfulness, answer_relevancy])

    if evaluate_retrieval:
        try:
            from ragas.metrics import context_precision, context_recall
        except ImportError:
            from ragas.metrics import ContextPrecision, ContextRecall
            context_precision = ContextPrecision()
            context_recall = ContextRecall()
        metrics.extend([context_precision, context_recall])

    if not metrics:
        return {"scores": {}, "details": records, "test_count": len(records)}

    # ---- ④ 设裁判 LLM ----
    evaluator_llm = LangchainLLMWrapper(judge_llm)
    for m in metrics:
        m.llm = evaluator_llm

    # ---- ⑤ 执行评估 ----
    try:
        result = evaluate(dataset, metrics=metrics)
    except Exception as e:
        return {
            "error": f"评估执行失败: {e}",
            "scores": None,
            "details": records,
        }

    # ---- ⑥ 整理结果 ----
    scores = {}
    detail_rows = []

    try:
        df = result.to_pandas()

        # 汇总各指标均值
        for col in df.columns:
            if col not in ("question", "answer", "contexts", "ground_truth"):
                series = df[col].dropna()
                if len(series) > 0:
                    scores[col] = round(float(series.mean()), 4)

        # 逐条明细
        for i, r in enumerate(records):
            row = {
                "question": r["question"],
                "answer": r["answer"][:200],
            }
            for col in df.columns:
                if col not in ("question", "answer", "contexts", "ground_truth"):
                    val = df[col].iloc[i] if i < len(df) else None
                    if val is not None and not (isinstance(val, float) and str(val) == "nan"):
                        row[col] = round(float(val), 4)
            detail_rows.append(row)

    except Exception:
        # to_pandas 失败的话，尝试直接读 result dict
        for key, value in result.items():
            if key not in ("question", "answer", "contexts", "ground_truth"):
                try:
                    scores[key] = round(float(value), 4)
                except (TypeError, ValueError):
                    pass

        for r in records:
            detail_rows.append({"question": r["question"], "answer": r["answer"][:200]})

    return {
        "scores": scores,
        "details": detail_rows,
        "test_count": len(records),
    }
