# coding: utf-8
import random

digits = "0123456789abcdefghijklmnopqrstuvwxyz"
digits2 = "~!@#$%^&*()-+=][}{|:;<>,.?"


def convert(number, base):  # 将number转base进制
    result_str = ""

    while number > 0:
        remainder = number % base
        result_str = digits[remainder] + result_str
        number //= base

    return result_str


def generate_random_string():  # 返回2~3随机长度字符串
    length = random.randint(2, 3)
    random_string = "".join(random.choice(digits2) for _ in range(length))
    return random_string


def lock(src):
    result = ""
    record_str = ""

    for i in src:
        random_num = random.randint(11, 36)
        temp_str = convert(ord(i), random_num)
        record_str += digits[random_num - 1]
        result += temp_str + generate_random_string()

    random_num = random.randint(11, 36)
    result = (
        convert(
            len(record_str) + 88, random_num
        )  # 88是随便写的，加它的原因是如果进制位<36，那么转36进制内任意一个都是相同的结果，会降低破解难度
        + result[::-1]
        + record_str
        + digits[random_num - 1]
    )

    return result