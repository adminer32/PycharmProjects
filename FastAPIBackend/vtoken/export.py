from .lock_V2 import lock
from .unlock_V2 import unlock
from .encryption import aes_encrypt, aes_decrypt
import json
import time
import urllib.parse

def encode(payload: dict) -> str:
    if payload is None or not isinstance(payload, dict):
        return ""
    
    payload["exp"] = int(time.time() + 900)
    src = json.dumps(payload, separators=(",", ":"))
    locked = lock(src)
    encrypted = aes_encrypt(locked)
    result = urllib.parse.quote(encrypted)
    return result

def decode(token: str) -> dict:
    if token is None or not isinstance(token, str):
        return {}
    
    try:
        token = urllib.parse.unquote(token)
        decrypted = aes_decrypt(token)
        src = unlock(decrypted)
        payload = json.loads(src)
        exp = payload.get("exp", 0)
        payload.pop("exp", None)
        result = {
            "data": payload,
        }
        
        if exp < int(time.time()):
            result["success"] = False
        else:
            result["success"] = True
        
        return result
    except:
        return {"success": False}