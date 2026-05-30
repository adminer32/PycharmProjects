const noticeHeight = 50;
const noticeGap = 20;

let noticeList = [];
let yPosList = [];
let maxNoticeCount = 0;

class FloatNotice extends HTMLElement {

  static get observedAttributes() {
    return ["value", "type", "yPos"];
  }

  constructor() {
    super();

    this._root = this.attachShadow({ mode: "open" });

    this._root.innerHTML = `
      <style>
        .float-notice {
          position: fixed;
          right: 20px;
          z-index: 9999;
        }

        .notice-body {
          padding: 10px 16px;
          border-radius: 8px;
          box-shadow: 5px 5px 10px rgba(0,0,0,0.3);
          color: white;
          font-weight: bold;

          /* 初始状态（在右侧外面） */
          transform: translateX(120%);
          opacity: 0;

          transition:
            transform 0.4s cubic-bezier(0.16, 1, 0.3, 1),
            opacity 0.3s ease;
        }

        /* 出现状态 */
        .notice-body.show {
          transform: translateX(0);
          opacity: 1;
        }

        /* 消失状态 */
        .notice-body.hide {
          transform: translateX(120%);
          opacity: 0;
        }

        .info { background: rgb(0,128,255); }
        .success { background: rgb(0,200,0); }
        .warning { background: rgb(255,200,0); }
        .error { background: rgb(255,0,0); }

        h3 { margin: 0; }
      </style>

      <div class="float-notice">
        <div class="notice-body">
          <h3></h3>
        </div>
      </div>
    `;
  }

  connectedCallback() {
    this.update();
  }

  attributeChangedCallback() {
    this.update();
  }

  update() {
    const type = this.getAttribute("type") || "info";
    const value = this.getAttribute("value") || "";
    const yPos = this.getAttribute("yPos") || "20";

    const container = this._root.querySelector(".float-notice");
    const body = this._root.querySelector(".notice-body");
    const title = this._root.querySelector("h3");

    container.style.top = `${yPos}px`;
    body.className = "notice-body " + type;
    title.textContent = value;
  }
}

function calculateMaxNoticeCount() {
  const windowHeight = window.innerHeight;
  maxNoticeCount = Math.floor((windowHeight - noticeHeight) / (noticeHeight + noticeGap));
  yPosList = [];
  for (let i = 0; i < maxNoticeCount; i++) {
    yPosList.push(i * (noticeHeight + noticeGap) + 20);
  }
}

function calculateYPos() {
  // 计算 y 坐标
  let yPos = 0;
  if (noticeList.length > 0) {
    const lastNotice = noticeList[noticeList.length - 1];
    const lastYPos = parseInt(lastNotice.getAttribute("yPos"));
    const lastIndex = yPosList.indexOf(lastYPos);
    if (lastIndex === -1) {
      yPos = yPosList[0];
    } else {
      yPos = yPosList[lastIndex + 1];
    }
  } else {
    yPos = 20;
  }
  return yPos;
}

customElements.define("float-notice", FloatNotice);
calculateMaxNoticeCount(); // 立即计算一次可用的 y 坐标

// 监听窗口高度变化，更新可用 y 坐标
window.addEventListener("resize", calculateMaxNoticeCount);

/* 暴露全局函数 */
window.showNotice = function(message, type, duration = 3000) {
  const notice = document.createElement("float-notice");
  const yPos = calculateYPos();
  notice.setAttribute("type", type);
  notice.setAttribute("value", message);
  notice.setAttribute("yPos", yPos);
  document.body.appendChild(notice);
  noticeList.push(notice);
  yPosList.push(parseInt(notice.getAttribute("yPos")));

  const body = notice.shadowRoot.querySelector(".notice-body");

  // 下一帧再加 show，保证动画触发
  requestAnimationFrame(() => {
    body.classList.add("show");
  });

  // 3秒后触发消失
  setTimeout(() => {
    body.classList.remove("show");
    body.classList.add("hide");

    // 等动画结束再删除 DOM
    setTimeout(() => {
      notice.remove();
      noticeList.splice(noticeList.indexOf(notice), 1);
      yPosList.splice(yPosList.indexOf(parseInt(notice.getAttribute("yPos"))), 1);
    }, 400);
  }, duration);
};