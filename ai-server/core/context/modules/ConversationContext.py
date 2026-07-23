"""会话历史"""
from typing import List, Tuple
from core.context.modules.BaseContext import BaseContext

class ConversationContext(BaseContext):
    def __init__(self, max_turns=10, priority=80):
        super().__init__("Conversation", priority)
        self.max_turns = max_turns
        self._history: List[Tuple[str,str]] = []

    def add(self, role: str, content: str):
        self._history.append((role, content))
        while len(self._history) > self.max_turns * 2:
            self._history.pop(0)

    def clear(self): self._history.clear()

    def trim(self, keep: int):
        self._history = self._history[-(keep * 2):]

    def render(self):
        if not self._history: return ""
        return "## 对话历史\n" + "\n".join(
            f"{'用户' if r=='user' else '助手'}: {c}" for r, c in self._history
        )

    def token_count(self): return sum(len(c) for _, c in self._history) // 4
