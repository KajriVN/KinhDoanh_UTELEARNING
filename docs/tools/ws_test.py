# Test chat realtime: học viên Cường gửi tin -> giảng viên An nhận được qua STOMP
import json, re, time, threading
import urllib.request, urllib.parse, http.cookiejar
import websocket

B = "http://localhost:8090"


def login(email, pw):
    cj = http.cookiejar.CookieJar()
    op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
    html = op.open(B + "/auth/login").read().decode()
    csrf = re.search(r'name="_csrf" value="([^"]+)"', html).group(1)
    data = urllib.parse.urlencode({"_csrf": csrf, "email": email, "password": pw}).encode()
    op.open(B + "/auth/login", data)
    return "; ".join(f"{c.name}={c.value}" for c in cj)


def frame(cmd, headers=None, body=""):
    h = "".join(f"{k}:{v}\n" for k, v in (headers or {}).items())
    return f"{cmd}\n{h}\n{body}\x00"


def client(cookie, name, inbox):
    ws = websocket.create_connection("ws://localhost:8090/ws/websocket", header=["Cookie: " + cookie])
    ws.send(frame("CONNECT", {"accept-version": "1.2", "host": "localhost"}))
    assert ws.recv().startswith("CONNECTED"), name + " connect fail"
    ws.send(frame("SUBSCRIBE", {"id": "sub-0", "destination": "/user/queue/messages"}))
    ws.send(frame("SUBSCRIBE", {"id": "sub-1", "destination": "/user/queue/errors"}))

    def loop():
        while True:
            try:
                msg = ws.recv()
            except Exception:
                return
            if msg.startswith("MESSAGE"):
                inbox.append(msg.split("\n\n", 1)[1].rstrip("\x00"))
    threading.Thread(target=loop, daemon=True).start()
    return ws


stu_inbox, gv_inbox, out_inbox = [], [], []
stu = client(login("cuong.le@gmail.com", "123456"), "student", stu_inbox)
gv = client(login("an.nguyen@utelearn.vn", "123456"), "instructor", gv_inbox)
outsider = client(login("dung.pham@gmail.com", "123456"), "outsider", out_inbox)
time.sleep(1)

# course 1 = Spring Boot (GV An), student 4 = Cường
stu.send(frame("SEND", {"destination": "/app/chat.send", "content-type": "application/json"},
               json.dumps({"courseId": 1, "studentId": 4, "content": "Hello thay, test realtime"})))
time.sleep(1.5)
gv.send(frame("SEND", {"destination": "/app/chat.send", "content-type": "application/json"},
              json.dumps({"courseId": 1, "studentId": 4, "content": "Thay nhan duoc roi"})))
# Dung không thuộc cuộc trò chuyện -> phải bị từ chối
outsider.send(frame("SEND", {"destination": "/app/chat.send", "content-type": "application/json"},
                    json.dumps({"courseId": 1, "studentId": 4, "content": "hack"})))
time.sleep(1.5)

print("student inbox:", stu_inbox)
print("instructor inbox:", gv_inbox)
print("outsider inbox:", out_inbox)
ok = (len(stu_inbox) == 2 and len(gv_inbox) == 2 and not any("hack" in m for m in stu_inbox + gv_inbox))
print("RESULT:", "PASS" if ok else "FAIL")
