"""
上下文管理器 (ContextManager)

统一管理所有上下文模块：注册 → 渲染 → 压缩 → 组装 → 注入 LLM。
"""

from typing import Dict, List, Optional

from langchain_core.messages import BaseMessage, HumanMessage, SystemMessage

from core.context.modules.BaseContext import BaseContext
from utils.logger import get_logger

logger = get_logger(__name__)
logger.debug("模块加载: core.context.ContextManager")


class ContextManager:
    """统一上下文管理器

    解决上下文污染/干扰/冲突/混淆四类问题：
      - 按优先级排序渲染
      - 超出 token 上限时裁剪低优先级模块
    """

    def __init__(self, max_tokens: int = 6000):
        self._modules: Dict[str, BaseContext] = {}
        self.max_tokens = max_tokens
        logger.info(f"ContextManager 创建 | token上限={max_tokens}")

    # ============================================================
    # 注册 / 查询
    # ============================================================

    def register(self, m: BaseContext):
        """注册上下文模块"""
        self._modules[m.name] = m
        logger.info(
            f"上下文注册: {m.name} | 优先级={m.priority} | "
            f"当前共{len(self._modules)}个模块"
        )

    def unregister(self, name: str):
        """移除上下文模块"""
        if name in self._modules:
            del self._modules[name]
            logger.info(f"上下文移除: {name} | 剩余{len(self._modules)}个模块")
        else:
            logger.warning(f"上下文移除失败: {name} 不存在")

    def get_module(self, name: str) -> Optional[BaseContext]:
        """获取指定上下文模块"""
        m = self._modules.get(name)
        if m is None:
            logger.debug(f"get_module: {name} → None (可用: {list(self._modules.keys())})")
        return m

    def total_tokens(self) -> int:
        """计算所有模块 token 总数"""
        total = sum(m.token_count() for m in self._modules.values())
        logger.debug(f"total_tokens: {total}")
        return total

    # ============================================================
    # 压缩
    # ============================================================

    def compress(self, target: int):
        """压缩上下文到目标 token 数"""
        current = self.total_tokens()
        if current <= target:
            logger.debug(f"compress: 无需压缩 ({current} ≤ {target})")
            return

        logger.warning(
            f"上下文压缩开始 | {current} → {target} tokens | "
            f"需削减{current - target}tokens"
        )

        from core.context.modules.ConversationContext import ConversationContext

        rounds = 0
        while self.total_tokens() > target:
            compressed = False
            for m in sorted(self._modules.values(), key=lambda x: x.priority):
                if isinstance(m, ConversationContext) and m.token_count() > 0:
                    old_count = len(m._history)
                    m.trim(max(1, old_count // 2 - 1))
                    new_count = len(m._history)
                    if new_count < old_count:
                        logger.debug(
                            f"  压缩 ConversationContext: "
                            f"{old_count}→{new_count}条 ({m.token_count()}tokens)"
                        )
                        compressed = True
                    if len(m._history) <= 2:
                        break
                    break
            if not compressed:
                logger.warning("compress: 无法继续压缩，已到极限")
                break
            rounds += 1

        logger.info(
            f"上下文压缩完成 | {current}→{self.total_tokens()}tokens | "
            f"{rounds}轮压缩"
        )

    # ============================================================
    # 组装
    # ============================================================

    def assemble(self, max_tokens: Optional[int] = None) -> List[BaseMessage]:
        """组装最终上下文，返回 LLM 消息列表"""
        target = max_tokens or self.max_tokens
        logger.info(
            f"assemble 开始 | token上限={target} | "
            f"已注册{len(self._modules)}个模块"
        )

        # 按优先级降序渲染
        sorted_modules = sorted(
            self._modules.values(), key=lambda x: -x.priority
        )

        parts = []
        module_details = []
        for m in sorted_modules:
            rendered = m.render()
            if rendered:
                parts.append(rendered)
                module_details.append(
                    f"{m.name}(p={m.priority},t={m.token_count()})"
                )

        context = "\n\n".join(parts)
        est_tokens = len(context) // 4

        logger.info(
            f"assemble 渲染完成 | ~{est_tokens}tokens | "
            f"模块: [{', '.join(module_details)}]"
        )

        # 超限则压缩后重新渲染
        if est_tokens > target:
            overflow = est_tokens - target
            logger.warning(
                f"assemble 超限 | {est_tokens}>{target} | 溢出{overflow}tokens"
            )
            self.compress(target)

            parts = []
            for m in sorted_modules:
                rendered = m.render()
                if rendered:
                    parts.append(rendered)
            context = "\n\n".join(parts)
            est_tokens = len(context) // 4
            logger.info(f"assemble 压缩后 | ~{est_tokens}tokens")

        # 组装消息
        system_content = context if context.strip() else "你是非遗知识助手。"
        messages: List[BaseMessage] = [SystemMessage(content=system_content)]

        logger.info(
            f"assemble 完成 | {len(messages)}条消息 | "
            f"SystemMessage {len(system_content)}字符(~{len(system_content)//4}tokens)"
        )
        return messages

    def add_user_message(self, msgs: List[BaseMessage], question: str) -> List[BaseMessage]:
        """在消息列表末尾追加用户问题"""
        msgs.append(HumanMessage(content=question))
        logger.debug(f"add_user_message: {len(question)}字符 | 总共{len(msgs)}条消息")
        return msgs
