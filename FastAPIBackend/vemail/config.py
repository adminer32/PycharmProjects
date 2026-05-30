import json
from utils.load_config import Config
from pathlib import Path
import os

BASE_DIR = Path(__file__).resolve().parent

with open("configs/sendgrid.json", "r", encoding="utf-8") as f:
    data = json.load(f)

with open(os.path.join(BASE_DIR, "templates/verify_email.html"), "r", encoding="utf-8") as f:
    verify_email_template = f.read()

data["html_template"] = verify_email_template
config = Config(data)