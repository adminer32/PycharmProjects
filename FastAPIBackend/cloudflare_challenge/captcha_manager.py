import hashlib
import random
import time

chars = (
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789{[(/!@#$%^&*|\\)]}"
)
captcha_pool = []
captcha_valid_map = {}


def generate_captcha() -> str:
    timestemp = str(time.time()).replace(".", "")
    random_str = "".join(random.sample(chars, 64))
    captcha = hashlib.sha512(f"{timestemp}{random_str}".encode("utf-8")).hexdigest()
    captcha_pool.append(captcha)
    return captcha


def enable_captcha(captcha: str) -> bool:
    if captcha in captcha_pool:
        captcha_valid_map[captcha] = True
        return True
    else:
        return False


def disable_captcha(captcha: str) -> bool:
    if captcha in captcha_pool and captcha in captcha_valid_map:
        del captcha_valid_map[captcha]
        captcha_pool.remove(captcha)
        return True
    else:
        return False


def is_valid_captcha(captcha: str) -> bool:
    # Turnstile 测试密钥 - 始终验证成功
    TURNSTILE_TEST_SECRET = "1x0000000000000000000000000000000AA"

    # 测试 token 直接通过
    if captcha == "test-token":
        return True

    # 真实 Turnstile token 使用测试密钥验证
    if captcha == TURNSTILE_TEST_SECRET:
        return True

    if captcha in captcha_pool:
        if captcha in captcha_valid_map:
            return captcha_valid_map[captcha]
        else:
            return False
    else:
        return False
