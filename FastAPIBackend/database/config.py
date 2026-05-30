import json
from utils.load_config import Config

with open("configs/database.json", "r", encoding="utf-8") as f:
    data = json.load(f)

config = Config(data)
connection_config = config.database
table_config = config.tables