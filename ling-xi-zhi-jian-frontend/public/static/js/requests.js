async function doPost(url = "", payload = {}, headers = {}) {
  headers["Content-Type"] = "application/json";
  try {
    const rsp = await fetch(url, {
      method: "POST",
      headers: headers,
      body: JSON.stringify(payload)
    });

    if (rsp.status >= 500) return null;

    const data = rsp.json();
    console.log(data);
    return data;
  } catch (error) {
    console.error("请求失败", error);
    return null;
  }
}

async function doGet(url = "", headers = {}) {
  try {
    const rsp = await fetch(url, {
        method: "GET",
        headers: headers
    });

    if (rsp.status >= 500) return null;

    const rspClone = rsp.clone();
    let data;
    try {
      data = await rsp.json();
      console.log(data);
    } catch  {
      data = await rspClone.text();
    }
    
    return data;
  } catch (error) {
    console.error("请求失败", error);
    return null;
  }
}

export { doPost, doGet };