import pymysql
from .config import connection_config as config

def get_connection() -> pymysql.Connection:
    conn = pymysql.connect(
        host=config.host,             # 数据库地址
        port=config.port,             # 默认3306
        user=config.user,             # 用户名
        password=config.password,     # 密码
        database=config.database,     # 数据库名
        charset=config.charset,       # 编码
        cursorclass=pymysql.cursors.DictCursor  # 返回字典格式
    )
    return conn