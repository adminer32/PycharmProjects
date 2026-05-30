class Config:
    def __init__(self, data: dict) -> None:
        for key, value in data.items():
            if isinstance(value, dict):
                value = Config(value)
            setattr(self, key, value)