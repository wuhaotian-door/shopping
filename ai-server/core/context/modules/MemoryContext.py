"""用户记忆"""
from typing import Any, Dict, List
from core.context.modules.BaseContext import BaseContext

class MemoryContext(BaseContext):
    def __init__(self, priority=60):
        super().__init__("Memory", priority)
        self._m: Dict[str, Any] = {"preferences": [], "topics": [], "facts": []}

    def add_preference(self, cat: str):
        if cat not in self._m["preferences"]: self._m["preferences"].append(cat)

    def add_topic(self, topic: str):
        self._m["topics"].insert(0, topic)
        self._m["topics"] = self._m["topics"][:20]

    def add_fact(self, f: str):
        if f not in self._m["facts"]: self._m["facts"].append(f)

    def get_preferences(self) -> List[str]: return self._m["preferences"]
    def get_topics(self) -> List[str]: return self._m["topics"]

    def render(self):
        parts = []
        if self._m["preferences"]: parts.append(f"用户偏好: {', '.join(self._m['preferences'])}")
        if self._m["topics"]: parts.append(f"常问话题: {', '.join(self._m['topics'][:10])}")
        return "\n".join(parts)

    def token_count(self): return len(self.render()) // 4
