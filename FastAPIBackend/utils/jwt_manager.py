from __future__ import annotations
import jwt
import json
import time
import random
import hashlib
import database


class JWTManager:
    def __init__(self: JWTManager) -> None:
        with open("configs/jwt.json", "r", encoding="utf-8") as f:
            self.json = json.load(f)

    def create_token(self: JWTManager, user_id: int, ip_address: str) -> dict:
        token_id = hashlib.sha256(
            (str(user_id) + str(random.random()) + str(time.time())).encode()
        ).hexdigest()
        access_token = self.create_access_token(user_id, token_id)
        refresh_token = self.create_refresh_token(token_id)
        database.insert_jwt_ip(token_id, ip_address)
        return {"access_token": access_token, "refresh_token": refresh_token}

    def create_access_token(self: JWTManager, user_id: int, token_id: str) -> str:
        payload = {
            "user_id": user_id,
            "exp": int(time.time()) + self.json["expiration_time"],
            "iat": int(time.time()),
            "jti": token_id,
        }
        token = jwt.encode(
            payload, self.json["secret"], algorithm=self.json["algorithm"]
        )
        return token

    def create_refresh_token(self: JWTManager, token_id: str) -> str:
        payload = {
            "token_id": token_id,
            "exp": int(time.time()) + self.json["refresh_expiration_time"],
            "iat": int(time.time()),
            "jti": hashlib.sha256(
                (str(token_id) + str(random.random())).encode()
            ).hexdigest(),
        }
        token = jwt.encode(
            payload, self.json["secret"], algorithm=self.json["algorithm"]
        )
        return token

    def verify_access_token(
        self: JWTManager, token: str, ip_address: str, role: str
    ) -> int | None:
        try:
            payload = jwt.decode(
                token, self.json["secret"], algorithms=[self.json["algorithm"]]
            )
            user_id = payload["user_id"]
            token_id = payload["jti"]

            result = database.get_jwt_blacklist(token_id)
            success = result.get("success", False)
            if success:
                return None

            result = database.get_jwt_ip(token_id)
            success = result.get("success", False)
            if not success:
                return None

            info = result.get("info")
            if info.get("ip_address") != ip_address:
                return None

            result = database.get_user_by_id(user_id)
            success = result.get("success", False)
            if not success:
                return None

            user_info = result.get("info")
            if user_info.get("role") != role.upper():
                return None

            return user_id
        except:
            return None

    def verify_refresh_token(
        self: JWTManager, token: str, ip_address: str
    ) -> str | None:
        try:
            payload = jwt.decode(
                token, self.json["secret"], algorithms=[self.json["algorithm"]]
            )
            token_id = payload["jti"]

            result = database.get_jwt_blacklist(token_id)
            success = result.get("success", False)
            if success:
                return None

            result = database.get_jwt_ip(token_id)
            success = result.get("success", False)
            if not success:
                return None

            info = result.get("info")
            if info.get("ip_address") != ip_address:
                return None

            return token_id
        except:
            return None

    def refresh_access_token(
        self: JWTManager, access_token: str, refresh_token: str, ip_address: str
    ) -> dict:
        try:
            token_id = self.verify_refresh_token(refresh_token, ip_address)
            user_id = jwt.decode(access_token, options={"verify_signature": False}).get(
                "user_id"
            )
            if not token_id:
                return {"success": False, "token": {}}

            if not user_id:
                return {"success": False, "token": {}}

            result = database.get_jwt_blacklist(token_id)
            success = result.get("success", False)
            if success:
                return {"success": False, "token": {}}

            database.insert_jwt_blacklist(token_id, access_token, refresh_token)
            token = self.create_token(user_id, ip_address)
            return {"success": True, "token": token}
        except:
            return {"success": False, "token": {}}
