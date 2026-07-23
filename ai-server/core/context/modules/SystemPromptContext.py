"""系统提示词"""
from core.context.modules.BaseContext import BaseContext

class SystemPromptContext(BaseContext):
    def __init__(self, prompt: str = "", priority: int = 100):
        super().__init__("SystemPrompt", priority)
        self.prompt = prompt or (
            "你是'码上非遗'智能助手，精通中国非物质文化遗产。\n"
            "规则:\n"
            "1. 用户问历史/工艺/特点 → search_knowledge\n"
            "2. 用户问传承人/老师 → search_inheritors\n"
            "3. 用户问活动/体验/课程 → search_projects\n"
            "4. 用户问商品/礼物 → search_products\n"
            "5. 用户说'推荐'或模糊意图 → recommend_content\n"
            "6. 闲聊直接回答\n"
            "7. 回答标注来源，搜不到如实告知\n"
        )
    def render(self): return self.prompt
    def token_count(self): return len(self.prompt) // 4
