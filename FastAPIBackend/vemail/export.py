from sendgrid import SendGridAPIClient
from sendgrid.helpers.mail import Mail, Email
from .config import config
import colorama
colorama.init()

SENDGRID_API_KEY = config.api_key
FROM_NAME = config.from_name
FROM_EMAIL = config.from_email
SUBJECT = config.subject
HTML_TEMPLATE = config.html_template
ACTIVATION_LINK = config.activation_link

def send(to_email: str, vtoken: str) -> None:
    if not vtoken:
        print(f"{colorama.Fore.RED}ERROR{colorama.Fore.RESET}:  \ttoken 为空")
        return
    
    message = Mail(
        from_email=Email(email=FROM_EMAIL, name=FROM_NAME),
        to_emails=to_email,
        subject=SUBJECT,
        html_content=HTML_TEMPLATE.replace("{{ACTIVATION_LINK}}", ACTIVATION_LINK.format(vtoken=vtoken))
    )

    try:
        sg = SendGridAPIClient(SENDGRID_API_KEY)
        response = sg.send(message)
        print(f"{colorama.Fore.GREEN}EMAIL{colorama.Fore.RESET}:  \t邮件发送成功，状态码 {response.status_code}")
    except Exception as e:
        print(f"{colorama.Fore.RED}ERROR{colorama.Fore.RESET}:  \t邮件发送失败 {e}")

