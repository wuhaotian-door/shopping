"""基础上下文抽象"""
from abc import ABC, abstractmethod

class BaseContext(ABC):
    def __init__(self, name: str, priority: int = 0):
        self.name = name; self.priority = priority
    @abstractmethod
    def render(self) -> str: ...
    @abstractmethod
    def token_count(self) -> int: ...
    def __repr__(self): return f"{self.name}(p={self.priority}, t={self.token_count()})"
