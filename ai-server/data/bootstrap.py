"""
启动时数据同步: 主后端 MySQL → 本地 SQLite → .md 文件 → Chroma 向量化

依赖: pip install pymysql

流程:
  1. 连接主后端 MySQL (code1st 库)
  2. 建本地 SQLite (data/ai_data.db)
  3. 同步 4 类业务数据: 知识文章 / 传承人 / 项目 / 商品
  4. 导出 SQLite → data/{knowledge,inheritors,projects,products}/*.md
  5. 调用 knowledge_base.build_all() 完成分块 + 向量化
"""

import os
import sqlite3
import time

from utils.logger import get_logger

logger = get_logger(__name__)

BASE = os.path.dirname(os.path.abspath(__file__))
SQLITE_PATH = os.path.join(BASE, "ai_data.db")
DATA_DIRS = {
    "knowledge": os.path.join(BASE, "knowledge"),
    "inheritors": os.path.join(BASE, "inheritors"),
    "projects": os.path.join(BASE, "projects"),
    "products": os.path.join(BASE, "products"),
}

# ============================================================
# MySQL → SQLite 同步配置
# ============================================================

SYNC_TABLES = [
    {
        "name": "knowledge_article",
        "dir": "knowledge",
        "sql": """
            SELECT title, CONCAT('分类：', IFNULL(category,''), '\n\n', IFNULL(content,''))
            FROM knowledge_article WHERE status = 1
        """,
    },
    {
        "name": "inheritor",
        "dir": "inheritors",
        "sql": """
            SELECT
                i.name,
                CONCAT(
                    '地区：', IFNULL(i.region,''), '\n',
                    '擅长：', IFNULL(p.craft_types,''), '\n',
                    CASE WHEN p.honor_title IS NOT NULL AND p.honor_title != ''
                         THEN CONCAT('荣誉称号：', p.honor_title, '\n') ELSE '' END,
                    '\n', IFNULL(p.self_intro, '')
                )
            FROM inheritor i
            LEFT JOIN inheritor_profile p ON p.employee_id = i.id
            WHERE i.status = 1
        """,
    },
    {
        "name": "heritage_project",
        "dir": "projects",
        "sql": """
            SELECT
                title,
                CONCAT(
                    '分类：', IFNULL(category,''), '\n',
                    '地点：', IFNULL(location,''), '\n',
                    CASE WHEN start_date IS NOT NULL
                         THEN CONCAT('时间：', start_date, ' 至 ', IFNULL(end_date,''))
                         ELSE '' END, '\n',
                    CASE WHEN price > 0 THEN CONCAT('费用：¥', price, '\n') ELSE '费用：免费\n' END,
                    '\n', IFNULL(description,'')
                )
            FROM heritage_project WHERE status = 1
        """,
    },
    {
        "name": "product",
        "dir": "products",
        "sql": """
            SELECT
                name,
                CONCAT(
                    '分类：', IFNULL(category,''), '\n',
                    '价格：¥', price, '\n',
                    CASE WHEN inheritor_name IS NOT NULL AND inheritor_name != ''
                         THEN CONCAT('传承人：', inheritor_name, '\n') ELSE '' END,
                    '\n', IFNULL(description,'')
                )
            FROM product WHERE status = 1
        """,
    },
]


def bootstrap(mysql_config: dict):
    """执行完整的数据同步 + 知识库构建流程"""
    logger.info("=" * 50)
    logger.info("数据同步开始: MySQL → SQLite → .md → Chroma")
    t0 = time.time()

    # ---- Step 1: 连 MySQL, 建 SQLite ----
    try:
        import pymysql
    except ImportError:
        logger.error("缺少 pymysql，跳过数据同步。安装: pip install pymysql")
        return

    conn_mysql = pymysql.connect(
        host=mysql_config["host"],
        port=mysql_config["port"],
        user=mysql_config["user"],
        password=mysql_config["password"],
        database=mysql_config["database"],
        charset="utf8mb4",
    )
    logger.info(
        f"MySQL 连接成功: {mysql_config['user']}@"
        f"{mysql_config['host']}:{mysql_config['port']}/{mysql_config['database']}"
    )

    # 重建 SQLite
    if os.path.exists(SQLITE_PATH):
        os.remove(SQLITE_PATH)
    conn_sqlite = sqlite3.connect(SQLITE_PATH)
    logger.info(f"SQLite 创建: {SQLITE_PATH}")

    total_docs = 0

    try:
        for tbl in SYNC_TABLES:
            logger.info(f"--- 同步 {tbl['name']} ---")

            # 建 SQLite 表
            conn_sqlite.execute(
                f"CREATE TABLE IF NOT EXISTS {tbl['name']} (title TEXT, content TEXT)"
            )
            conn_sqlite.execute(f"DELETE FROM {tbl['name']}")

            # 从 MySQL 读取
            cursor = conn_mysql.cursor()
            cursor.execute(tbl["sql"])
            rows = cursor.fetchall()
            cursor.close()
            logger.info(f"  MySQL 读取: {len(rows)} 条")

            if not rows:
                logger.warning(f"  {tbl['name']} 无数据，跳过")
                continue

            # 写入 SQLite
            conn_sqlite.executemany(
                f"INSERT INTO {tbl['name']} (title, content) VALUES (?, ?)",
                rows,
            )
            conn_sqlite.commit()

            # 导出 .md
            out_dir = DATA_DIRS[tbl["dir"]]
            os.makedirs(out_dir, exist_ok=True)
            # 清旧文件
            for f in os.listdir(out_dir):
                if f.endswith(".md"):
                    os.remove(os.path.join(out_dir, f))

            for title, content in rows:
                fname = title.replace("/", "_").replace("\\", "_").replace(":", "：")
                fname = fname[:80] + ".md"  # 文件名截断
                path = os.path.join(out_dir, fname)
                with open(path, "w", encoding="utf-8") as f:
                    f.write(f"# {title}\n\n{content or ''}")
            logger.info(f"  .md 导出: {len(rows)} 篇 → {out_dir}")
            total_docs += len(rows)

    finally:
        conn_mysql.close()
        conn_sqlite.close()

    logger.info(f"数据同步完成: 共 {total_docs} 篇文档, 耗时 {time.time()-t0:.1f}s")

    # ---- Step 2: 构建 Chroma 知识库（已有数据则跳过） ----
    if total_docs > 0:
        # 检查 Chroma 是否已有数据
        chroma_dir = os.path.join(BASE, "..", "chroma_db")
        chroma_exists = os.path.exists(chroma_dir) and os.path.exists(
            os.path.join(chroma_dir, "chroma.sqlite3"))
        if chroma_exists:
            logger.info("Chroma 已有索引，跳过重建（如需重建请调 POST /ai/knowledge/reload）")
        else:
            logger.info("开始构建知识库向量索引...")
            from core.tool.knowledge_base import build_all
            build_all()
    else:
        logger.warning("无数据，跳过知识库构建")

    logger.info("=" * 50)
