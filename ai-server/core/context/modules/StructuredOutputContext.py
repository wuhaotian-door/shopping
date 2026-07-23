"""结构化输出约束"""
from typing import Any, Dict, Optional
from core.context.modules.BaseContext import BaseContext
import json

class StructuredOutputContext(BaseContext):
    def __init__(self, schema: dict = None, priority=95):
        super().__init__("StructuredOutput", priority)
        self.schema = schema or {}

    def render(self):
        if not self.schema: return ""
        return "## 输出格式\n请按以下 JSON Schema 输出:\n```json\n" + json.dumps(self.schema, ensure_ascii=False, indent=2) + "\n```"

    def token_count(self): return len(self.render()) // 4
