"""提示词管理"""
from typing import Dict, Optional

class PromptManager:
    def __init__(self):
        self._prompts: Dict[str, Dict[str, str]] = {"system": {}, "rag": {}, "recommend": {}, "eval": {}}

    def register(self, cat, name, tpl):
        self._prompts.setdefault(cat, {})[name] = tpl

    def render(self, cat, name, **kw):
        t = self._prompts.get(cat, {}).get(name, "")
        return t.format(**kw) if t and kw else t

_pm = PromptManager()
_pm.register("system", "default",
    "你是'码上非遗'智能助手，精通中国非物质文化遗产。\n"
    "规则: 1)问历史/工艺→search_knowledge 2)问传承人→search_inheritors "
    "3)问活动→search_projects 4)问商品→search_products 5)推荐意图→recommend_content "
    "6)闲聊直接回答。回答标注来源，搜不到如实告知。")
_pm.register("recommend", "default", "根据用户偏好 {preferences}，推荐 {limit} 个非遗内容。")

def get_prompt_manager(): return _pm
