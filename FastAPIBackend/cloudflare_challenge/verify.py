from __future__ import annotations
import requests
import json


class CloudflareChallenge:
    def __init__(self: CloudflareChallenge) -> None:
        with open("configs/cloudflare.json", "r", encoding="utf-8") as f:
            config = json.load(f)

        self.secret = config["secret_key"]
        self.verify_url = config["api_url"]

    def verify_challenge(self: CloudflareChallenge, token: str) -> bool:
        veryfy_payload = {"secret": self.secret, "response": token}
        rsp = requests.post(self.verify_url, data=veryfy_payload)
        if rsp.status_code != 200:
            return False

        data = rsp.json()
        success = data.get("success", False)
        return success
