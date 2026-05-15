from functools import lru_cache
from pathlib import Path

from jinja2 import Template

PROMPTS_DIR = Path(__file__).parent.parent / "prompts"


@lru_cache(maxsize=32)
def load_system_prompt(task: str) -> str:
    """Load system prompt from file."""
    path = PROMPTS_DIR / task / "system.txt"
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def render_user_prompt(task: str, **kwargs: str) -> str:
    """Render user prompt using Jinja2 template."""
    path = PROMPTS_DIR / task / "user.jinja2"
    if not path.exists():
        return kwargs.get("prompt", "")
    tmpl = Template(path.read_text(encoding="utf-8"))
    return tmpl.render(**kwargs)
