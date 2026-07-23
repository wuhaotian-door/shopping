"""
工具管理器 (ToolManager)

职责:
  - 工具注册 / 发现
  - 工具执行 + 输出裁剪
  - 审批流标记
  - LLM 工具绑定
"""

import time
from typing import Any, Dict, List

from langchain_core.tools import BaseTool
from utils.logger import get_logger

logger = get_logger(__name__)
logger.debug("模块加载: core.tool.ToolManager")


class ToolManager:
    """统一管理 Agent 可调用的工具集"""

    def __init__(self, output_max: int = 1000):
        self._tools: Dict[str, BaseTool] = {}
        self._approval: set = set()
        self.output_max = output_max
        logger.debug(f"ToolManager 创建 | output_max={output_max}")

    # ============================================================
    # 注册 / 查询
    # ============================================================

    def register(self, tool: BaseTool, needs_approval: bool = False):
        """注册工具"""
        self._tools[tool.name] = tool
        if needs_approval:
            self._approval.add(tool.name)

        logger.info(
            f"工具注册: {tool.name} | "
            f"desc={tool.description[:60] if tool.description else '?'}... | "
            f"需审批={needs_approval} | 工具总数={len(self._tools)}"
        )

    def unregister(self, name: str):
        """移除工具"""
        if name in self._tools:
            del self._tools[name]
            self._approval.discard(name)
            logger.info(f"工具移除: {name} | 剩余{len(self._tools)}个")
        else:
            logger.warning(f"工具移除失败: {name} 未注册")

    def list_tools(self) -> List[BaseTool]:
        """列出所有工具"""
        tools = list(self._tools.values())
        logger.debug(f"list_tools: {len(tools)}个 → {[t.name for t in tools]}")
        return tools

    def get_tools_by_name(self) -> Dict[str, BaseTool]:
        """返回工具名 → 工具对象映射（供 tool_loop 使用）"""
        return dict(self._tools)

    def requires_approval(self, name: str) -> bool:
        """检查工具是否需要审批"""
        needs = name in self._approval
        logger.debug(f"requires_approval: {name} → {needs}")
        return needs

    # ============================================================
    # LLM 绑定
    # ============================================================

    def bind_to_llm(self, llm):
        """将工具列表绑定到 LLM"""
        tools = self.list_tools()
        tool_names = [t.name for t in tools]
        logger.info(f"bind_to_llm: {len(tools)}个工具 → {tool_names}")
        return llm.bind_tools(tools)

    # ============================================================
    # 执行
    # ============================================================

    def execute(self, name: str, **kwargs) -> str:
        """执行工具并裁剪输出"""
        tool = self._tools.get(name)
        if not tool:
            logger.error(f"execute: 未知工具 {name}")
            return f"错误: 未知工具 {name}"

        logger.info(
            f"execute: {name} | 参数: {str(kwargs)[:120]}..."
        )

        t0 = time.time()
        try:
            result = str(tool.invoke(kwargs))
            elapsed = time.time() - t0

            org_len = len(result)
            if org_len > self.output_max:
                result = result[:self.output_max] + (
                    f"\n...(输出已裁剪, 原始{org_len}字符)"
                )
                logger.info(
                    f"execute 完成: {name} | {org_len}→{len(result)}字符(已裁剪) | "
                    f"{elapsed:.2f}s"
                )
            else:
                logger.info(
                    f"execute 完成: {name} | {len(result)}字符 | {elapsed:.2f}s"
                )
            return result

        except Exception as e:
            elapsed = time.time() - t0
            logger.error(
                f"execute 失败: {name} | {elapsed:.2f}s | "
                f"{type(e).__name__}: {e}",
                exc_info=True,
            )
            return f"工具执行出错: {type(e).__name__}: {e}"
