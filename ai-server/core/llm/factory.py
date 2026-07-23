"""
LLM 工厂函数

根据参数创建 ChatOpenAI 实例，封装为 DeepSeekService。
"""

from langchain_openai import ChatOpenAI
from core.llm.services.deepseek_service import DeepSeekService
from utils.logger import get_logger

logger = get_logger(__name__)
logger.debug("模块加载: core.llm.factory")


def create_llm(
    model: str = "deepseek-chat",
    base_url: str = "https://api.deepseek.com",
    api_key: str = "",
    temperature: float = 0.4,
    max_tokens: int = 1024,
) -> DeepSeekService:
    """创建 LLM 服务实例"""
    logger.info(
        f"创建 LLM | model={model} | url={base_url} | "
        f"temp={temperature} | max_tok={max_tokens} | "
        f"key={'✓' if api_key else '✗ 缺失!'}"
    )

    if not api_key:
        logger.error("api_key 为空，ChatOpenAI 初始化将抛出异常!")

    try:
        llm = ChatOpenAI(
            model=model, base_url=base_url, api_key=api_key,
            temperature=temperature, max_tokens=max_tokens,
        )
        logger.info(f"ChatOpenAI 实例创建成功, model_name={llm.model_name}")
    except Exception as e:
        logger.error(f"ChatOpenAI 创建失败: {e}", exc_info=True)
        raise

    svc = DeepSeekService(llm)
    logger.debug(f"DeepSeekService 封装完成")
    return svc
