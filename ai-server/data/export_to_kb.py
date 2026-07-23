"""
将 MySQL 数据导出为知识库 .md 文件

用法: python data/export_to_kb.py
需要: pip install pymysql
"""

import os
import sys

try:
    import pymysql
except ImportError:
    print("请先安装: pip install pymysql")
    sys.exit(1)

# MySQL 连接配置
DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "your_password",  # ← 改成你的密码
    "database": "intangible_heritage",
    "charset": "utf8mb4",
}

# 导出的目标目录
OUT_DIR = os.path.dirname(os.path.abspath(__file__))

EXPORTS = [
    {
        "table": "knowledge_articles",
        "dir": "knowledge",
        "query": "SELECT title, content FROM knowledge_articles",
        "filename": lambda row: f"{row[0]}.md",
        "format": lambda row: f"# {row[0]}\n\n{row[1]}",
    },
    {
        "table": "inheritors",
        "dir": "inheritors",
        "query": """
            SELECT name, CONCAT(
                '荣誉称号：', IFNULL(honor_title,''), '\n',
                '地区：', IFNULL(region,''), '\n',
                CASE WHEN birth_year IS NOT NULL THEN CONCAT('出生：', birth_year, '年\n') ELSE '' END,
                '非遗项目：', craft, '\n\n',
                intro
            ) AS content FROM inheritors
        """,
        "filename": lambda row: f"{row[0]}.md",
        "format": lambda row: f"# {row[0]}\n\n{row[1]}",
    },
    {
        "table": "heritage_projects",
        "dir": "projects",
        "query": """
            SELECT title, CONCAT(
                '类别：', IFNULL(category,''), '\n',
                '地点：', IFNULL(location,''), '\n',
                '时间：', IFNULL(start_date,''), ' 至 ', IFNULL(end_date,''), '\n',
                '费用：', IF(price > 0, CONCAT('¥', price), '免费'), '\n',
                CASE WHEN inheritor_name != '' THEN CONCAT('指导老师：', inheritor_name, '\n') ELSE '' END,
                '\n', IFNULL(description,'')
            ) AS content FROM heritage_projects WHERE status = 1
        """,
        "filename": lambda row: f"{row[0]}.md",
        "format": lambda row: f"# {row[0]}\n\n{row[1]}",
    },
    {
        "table": "products",
        "dir": "products",
        "query": """
            SELECT name, CONCAT(
                '品类：', IFNULL(category,''), '\n',
                '价格：¥', price, '\n',
                '库存：', stock, '件\n',
                CASE WHEN craft != '' THEN CONCAT('关联非遗：', craft, '\n') ELSE '' END,
                '\n', IFNULL(description,'')
            ) AS content FROM products WHERE status = 1
        """,
        "filename": lambda row: f"{row[0]}.md",
        "format": lambda row: f"# {row[0]}\n\n{row[1]}",
    },
]


def main():
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    total = 0
    for exp in EXPORTS:
        out_dir = os.path.join(OUT_DIR, exp["dir"])
        os.makedirs(out_dir, exist_ok=True)

        # 清空旧文件
        for f in os.listdir(out_dir):
            if f.endswith(".md"):
                os.remove(os.path.join(out_dir, f))

        cursor.execute(exp["query"])
        rows = cursor.fetchall()

        count = 0
        for row in rows:
            fname = exp["filename"](row)
            # 文件名去非法字符
            fname = fname.replace("/", "_").replace("\\", "_").replace(":", "：")
            path = os.path.join(out_dir, fname)

            content = exp["format"](row)
            with open(path, "w", encoding="utf-8") as f:
                f.write(content)
            count += 1

        print(f"[{exp['dir']}] {count} 篇")
        total += count

    cursor.close()
    conn.close()
    print(f"\n导出完成，共 {total} 篇文档 → data/knowledge/, data/inheritors/, data/projects/, data/products/")
    print("随后调用 POST /ai/knowledge/reload 重建知识库即可。")


if __name__ == "__main__":
    main()
