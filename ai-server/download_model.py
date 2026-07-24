"""
一键下载 Embedding 模型到本地缓存（国内可用）

用法: python download_model.py

下载 paraphrase-multilingual-MiniLM-L12-v2（~400MB）
目标路径: ai-server/models/paraphrase-multilingual-MiniLM-L12-v2/

策略（按优先级）:
  ① ModelScope SDK（国内可直连）
  ② huggingface_hub + HF_ENDPOINT 镜像
  ③ 直接 HTTP 流式下载（HF_MIRROR）
  ④ 手动下载指引
"""
import os
import sys
import logging

# 加载 .env
try:
    from dotenv import load_dotenv
    load_dotenv(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env"))
except ImportError:
    pass

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger("download")

BASE = os.path.dirname(os.path.abspath(__file__))
MODEL_DIR = os.path.join(BASE, "models", "paraphrase-multilingual-MiniLM-L12-v2")
MODEL_ID = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
MIRROR = os.environ.get("HF_MIRROR", "https://hf-mirror.com")

os.makedirs(MODEL_DIR, exist_ok=True)


def check_done():
    if os.path.isfile(os.path.join(MODEL_DIR, "config.json")):
        logger.info(f"\n✅ 模型就绪: {MODEL_DIR}")
        return True
    return False


# ---- 方法 1: ModelScope ----
logger.info("=" * 50)
logger.info("方法 1: ModelScope SDK（国内可用）")
logger.info("=" * 50)
try:
    from modelscope import snapshot_download
    downloaded = snapshot_download(MODEL_ID, cache_dir=os.path.dirname(MODEL_DIR))
    downloaded = str(downloaded)
    logger.info(f"  ModelScope 缓存路径: {downloaded}")
    if os.path.isdir(downloaded) and os.path.abspath(downloaded) != os.path.abspath(MODEL_DIR):
        import shutil
        if os.path.exists(MODEL_DIR):
            shutil.rmtree(MODEL_DIR, ignore_errors=True)
        os.rename(downloaded, MODEL_DIR)
    if check_done():
        sys.exit(0)
except ImportError:
    logger.warning("  modelscope 未安装，跳过")
except Exception as e:
    logger.warning(f"  ❌ 失败: {e}")

# ---- 方法 2: huggingface_hub ----
logger.info("\n" + "=" * 50)
logger.info("方法 2: huggingface_hub + HF_ENDPOINT")
logger.info("=" * 50)
try:
    from huggingface_hub import snapshot_download
    snapshot_download(MODEL_ID, local_dir=MODEL_DIR)
    if check_done():
        sys.exit(0)
except Exception as e:
    logger.warning(f"  ❌ 失败: {e}")

# ---- 方法 3: 直接 HTTP 下载 ----
logger.info("\n" + "=" * 50)
logger.info("方法 3: 直接 HTTP 流式下载")
logger.info("=" * 50)

import requests

FILES = [
    "config.json", "tokenizer_config.json", "tokenizer.json",
    "special_tokens_map.json", "sentence_bert_config.json",
    "modules.json", "vocab.txt", "1_Pooling/config.json",
    "model.safetensors",
]

try:
    session = requests.Session()
    for fname in FILES:
        fdir = os.path.join(MODEL_DIR, os.path.dirname(fname))
        os.makedirs(fdir, exist_ok=True)
        fpath = os.path.join(MODEL_DIR, fname)
        if os.path.isfile(fpath):
            logger.info(f"  ⏭ {fname} (已存在)")
            continue
        url = f"{MIRROR}/{MODEL_ID}/resolve/main/{fname}"
        resp = session.get(url, timeout=120, allow_redirects=True, stream=True)
        if resp.status_code == 200:
            with open(fpath, "wb") as fout:
                for chunk in resp.iter_content(chunk_size=8192):
                    fout.write(chunk)
            size_mb = os.path.getsize(fpath) / (1024 * 1024)
            logger.info(f"  ✓ {fname} ({size_mb:.1f}MB)")
        else:
            logger.warning(f"  ✗ {fname} → HTTP {resp.status_code}")
    if check_done():
        sys.exit(0)
except Exception as e:
    logger.warning(f"  ❌ 失败: {e}")

# ---- 所有自动方法失败 ----
logger.info("\n" + "=" * 50)
logger.info("自动下载全部失败，请手动下载：")
logger.info("=" * 50)
logger.info(f"""
方案 A — 先装 modelscope 再运行本脚本:
  pip install modelscope
  python download_model.py

方案 B — 手动 hugginface-cli:
  set HF_ENDPOINT=https://hf-mirror.com
  huggingface-cli download {MODEL_ID} --local-dir "{MODEL_DIR}"

方案 C — 使用 hfd 脚本:
  git clone https://github.com/chenxwh/hfd.git
  cd hfd && ./hfd.sh {MODEL_ID} --tool aria2c --local-dir "{MODEL_DIR}"
""")
sys.exit(1)
