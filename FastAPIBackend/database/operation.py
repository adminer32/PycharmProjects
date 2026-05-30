from . import connect
import colorama

colorama.init()


def query(sql: str, params: tuple) -> list:
    conn = connect.get_connection()

    try:
        # 2️⃣ 创建游标
        with conn.cursor() as cursor:
            # 3️⃣ 执行 SQL
            cursor.execute(sql, params)  # 参数必须用 tuple
            rows = cursor.fetchall()  # 返回列表
    except Exception as e:
        print(f"{colorama.Fore.RED}ERROR{colorama.Fore.RESET}:  \t{e}")
        return []
    finally:
        conn.close()

    return rows


def change(sql: str, params: tuple) -> bool:
    conn = connect.get_connection()

    try:
        # 2️⃣ 创建游标
        with conn.cursor() as cursor:
            # 3️⃣ 执行 SQL
            cursor.execute(sql, params)  # 参数必须用 tuple
            success = cursor.rowcount > 0  # 判断是否执行成功
            conn.commit()  # 提交事务
        return success
    except Exception as e:
        print(f"{colorama.Fore.RED}ERROR{colorama.Fore.RESET}:  \t{e}")
        conn.rollback()  # 回滚事务
        return False
    finally:
        conn.close()


def insert_and_get_id(sql: str, params: tuple) -> int:
    """插入数据并返回自增ID"""
    conn = connect.get_connection()

    try:
        with conn.cursor() as cursor:
            cursor.execute(sql, params)
            conn.commit()
            return cursor.lastrowid if cursor.lastrowid else 0
    except Exception as e:
        print(f"{colorama.Fore.RED}ERROR{colorama.Fore.RESET}:  \t{e}")
        conn.rollback()
        return 0
    finally:
        conn.close()
