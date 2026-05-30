import json
from utils.load_config import Config

with open("configs/vtoken.json", "r", encoding="utf-8") as f:
    data = json.load(f)

config = Config(data)