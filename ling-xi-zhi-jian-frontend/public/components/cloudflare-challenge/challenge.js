import api from "/static/js/api.js";
import { doGet } from "/static/js/requests.js";

async function _getCaptcha() {
  const data = await doGet(api.public.challenge.captcha);
  if (data && data.success) {
    return data.captcha;
  } else {
    showNotice("获取验证码失败，请检查网络环境或稍后重试", "error");
    return null;
  }
}

async function _popChallengeWindow(captcha) {
  const width = 540;
  const height = 360;

  const left = (screen.width - width) / 2;
  const top = (screen.height - height) / 2;

  window.open(
    `/components/cloudflare-challenge/challenge.html?captcha=${captcha}`,
    "Cloudflare Bot Challenge",
    `width=${width},height=${height},left=${left},top=${top}`
  );

  return new Promise((resolve) => {
    window.addEventListener("message", function handler(event) {
      if (event.data.type === "captcha-result") {
        window.removeEventListener("message", handler)
        resolve(event.data.success)
      }
    });
  });
}

async function startChallenge(callbackFunction) {
  const captcha = await _getCaptcha();
  if (!captcha) return;

  const human = await _popChallengeWindow(captcha);

  if (!human) {
    showNotice("人机验证失败", "error");
    return;
  }

  callbackFunction(captcha);
}

export default startChallenge;