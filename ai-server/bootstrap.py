"""
启动时数据同步 — 数据层

流程:
  主后端 MySQL (code1st)
    → 本地 SQLite (data/ai_data.db)
    → Markdown 文件 (data/{knowledge,inheritors,projects,products}/)
    → Chroma 向量索引 (chroma_db/)

依赖: pip install pymysql
"""

import logging
import os
import sqlite3

logger = logging.getLogger("ai-server.bootstrap")

BASE = os.path.dirname(os.path.abspath(__file__))
SQLITE_PATH = os.path.join(BASE, "data", "ai_data.db")

# ============================================================
# 同步配置 — 4 张表 → 4 个集合
# ============================================================

SYNC_TABLES = [
    {
        "name": "knowledge_article",
        "dir": "knowledge",
        "sql": """
            SELECT
                title,
                CONCAT('分类：', IFNULL(category,''), '\n\n', IFNULL(content,''))
            FROM knowledge_article WHERE status = 1
        """,
    },
    {
        "name": "inheritor",
        "dir": "inheritors",
        # inheritor 与 inheritor_profile 做 LEFT JOIN 获取详细信息
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
                    '费用：', IF(price > 0, CONCAT('¥', price), '免费'), '\n',
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
                    '传承人：', IFNULL(inheritor_name,''), '\n',
                    '\n', IFNULL(description,'')
                )
            FROM product WHERE status = 1
        """,
    },
]

# 导出目录映射
DATA_DIRS = {t["dir"]: os.path.join(BASE, "data", t["dir"]) for t in SYNC_TABLES}


# ============================================================
# bootstrap — 主入口
# ============================================================

def bootstrap():
    """执行完整的数据同步 + 知识库构建流程"""
    logger.info("=" * 40)
    logger.info("数据同步开始")

    # ---- 检查 pymysql ----
    try:
        import pymysql
    except ImportError:
        logger.warning("pymysql 未安装，跳过数据同步。安装: pip install pymysql")
        return

    # ---- 连接主后端 MySQL ----
    mysql_cfg = {
        "host": os.getenv("MYSQL_HOST", "localhost"),
        "port": int(os.getenv("MYSQL_PORT", "3306")),
        "user": os.getenv("MYSQL_USER", "root"),
        "password": os.getenv("MYSQL_PASSWORD", ""),
        "database": os.getenv("MYSQL_DATABASE", "code1st"),
        "charset": "utf8mb4",
    }

    try:
        conn_mysql = pymysql.connect(**mysql_cfg)
    except Exception as e:
        logger.error(f"MySQL 连接失败: {e}")
        return

    # ---- 创建/清空本地 SQLite ----
    if os.path.exists(SQLITE_PATH):
        os.remove(SQLITE_PATH)
    conn_sqlite = sqlite3.connect(SQLITE_PATH)
    logger.info(f"MySQL 已连接 | SQLite: {SQLITE_PATH}")

    total = 0
    try:
        for tbl in SYNC_TABLES:
            # 建表 + 清空
            conn_sqlite.execute(
                f"CREATE TABLE IF NOT EXISTS {tbl['name']} (title TEXT, content TEXT)"
            )
            conn_sqlite.execute(f"DELETE FROM {tbl['name']}")

            # 从 MySQL 读取
            cur = conn_mysql.cursor()
            cur.execute(tbl["sql"])
            rows = cur.fetchall()
            cur.close()
            logger.info(f"  {tbl['name']}: {len(rows)} 条")

            if not rows:
                continue

            # 写入 SQLite
            conn_sqlite.executemany(
                f"INSERT INTO {tbl['name']} (title, content) VALUES (?, ?)", rows
            )
            conn_sqlite.commit()

            # 导出 .md 文件（先清旧文件）
            out_dir = DATA_DIRS[tbl["dir"]]
            os.makedirs(out_dir, exist_ok=True)
            for f in os.listdir(out_dir):
                if f.endswith(".md"):
                    os.remove(os.path.join(out_dir, f))

            for title, content in rows:
                # 文件名取标题，去非法字符，截断到 80 字符
                fname = ((title or "untitled")
                    .replace("/", "_").replace("\\", "_").replace(":", "："))[:80] + ".md"
                with open(os.path.join(out_dir, fname), "w", encoding="utf-8") as f:
                    f.write(f"# {title}\n\n{content or ''}")

            logger.info(f"  → {len(rows)} 篇 .md → {out_dir}")
            total += len(rows)

    finally:
        conn_mysql.close()
        conn_sqlite.close()

    logger.info(f"数据同步完成: {total} 篇文档")

    # ---- Chroma 向量化：已有索引则跳过（避免每次启动等 15s） ----
    if total > 0:
        chroma_db = os.path.join(BASE, "chroma_db", "chroma.sqlite3")
        if os.path.exists(chroma_db):
            logger.info("Chroma 索引已存在，跳过重建")
        else:
            logger.info("首次构建 Chroma 索引...")
            from tools import build_all
            build_all()

    logger.info("=" * 40)
