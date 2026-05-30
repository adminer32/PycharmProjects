import os
import base64
import hashlib
import random
import time

def save_avatar(avatar_data: str, role: str) -> str:
    if not avatar_data or not role:
        return None
    
    chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+[]{}|;:\",<.>/?！￥……（）——【】、；：‘’“”，。《》？あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをんがぎぐげござじずぜぞだぢづでどばびぶべぼぱぴぷぺぽぁぃぅぇぉゃゅょっー"
    random_str = "".join(random.sample(chars, 32))

    base64_head = avatar_data.split(",")[0]
    base64_data = avatar_data.split(",")[1]
    suffix = base64_head.split("/")[1].split(";")[0]
    suffix = suffix.split("+")[0] if "+" in suffix else suffix
    image_bytes = base64.b64decode(base64_data)

    filename = hashlib.sha1(f"{role}_TIMESTAMP{str(time.time() * 1000)}_RANDOM{random_str}_MD5{hashlib.md5(image_bytes).hexdigest()}_AVATAR").hexdigest() + "." + suffix

    if not os.path.exists(f"data/avatars/{role}"):
        os.makedirs(f"data/avatars/{role}")

    with open(f"data/avatars/{role}/{filename}", "wb") as f:
        f.write(image_bytes)
    
    return filename