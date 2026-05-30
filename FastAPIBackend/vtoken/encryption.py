import base64
import hashlib
from Crypto.Cipher import AES
from Crypto.Random import get_random_bytes
from .config import config

BLOCK_SIZE = 16  # AES block size
SECRET_KEY = config.secret_key

# PKCS7 padding
def pad(data: bytes) -> bytes:
    pad_len = BLOCK_SIZE - len(data) % BLOCK_SIZE
    return data + bytes([pad_len] * pad_len)


def unpad(data: bytes) -> bytes:
    pad_len = data[-1]
    if pad_len < 1 or pad_len > BLOCK_SIZE:
        raise ValueError("Invalid padding")
    return data[:-pad_len]


# 从明文密钥生成 AES key（取 sha512 前 32 字节）
def derive_key() -> bytes:
    return hashlib.sha512(SECRET_KEY.encode("utf-8")).digest()[:32]

# 加密
def aes_encrypt(plaintext: str) -> str:
    key_bytes = derive_key()

    iv = get_random_bytes(16)  # 随机 IV
    cipher = AES.new(key_bytes, AES.MODE_CBC, iv)

    padded = pad(plaintext.encode("utf-8"))
    encrypted = cipher.encrypt(padded)

    result = iv + encrypted
    return base64.b64encode(result).decode("utf-8")


# 解密
def aes_decrypt(ciphertext_b64: str) -> str:
    try:
        key_bytes = derive_key()

        raw = base64.b64decode(ciphertext_b64)

        iv = raw[:16]
        encrypted = raw[16:]

        cipher = AES.new(key_bytes, AES.MODE_CBC, iv)
        decrypted = cipher.decrypt(encrypted)

        return unpad(decrypted).decode("utf-8")
    except:
        return ""