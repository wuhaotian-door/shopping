"""工具定义上下文"""
from typing import Any, List
from core.context.modules.BaseContext import BaseContext

class ToolContext(BaseContext):
    def __init__(self, tools: List[Any] = None, priority=90):
        super().__init__("Tool", priority)
        self.tools = tools or []

    def render(self):
        if not self.tools: return ""
        lines = ["## 可用工具"]
        for t in self.tools:
            name = getattr(t, "name", t.__class__.__name__)
            desc = getattr(t, "description", "")
            lines.append(f"- {name}: {desc}")
        return "\n".join(lines)

    def token_count(self): return len(self.render()) // 4
