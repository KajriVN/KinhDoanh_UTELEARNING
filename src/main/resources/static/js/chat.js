// Chat realtime bằng SockJS + STOMP
// Luồng: gửi tin -> /app/chat.send -> server lưu DB -> đẩy về /user/queue/messages của 2 người
(function () {
    const box = document.getElementById('chat-box');
    const form = document.getElementById('chat-form');
    const input = document.getElementById('chat-input');
    const statusBadge = document.getElementById('status');

    const courseId = Number(box.dataset.courseId);
    const studentId = Number(box.dataset.studentId);
    const myId = Number(box.dataset.myId);

    let stompClient = null;

    function scrollToBottom() {
        box.scrollTop = box.scrollHeight;
    }

    function setStatus(text, color) {
        statusBadge.textContent = text;
        statusBadge.className = 'badge bg-' + color;
    }

    // dùng textContent để tránh bị chèn HTML (XSS)
    function appendMessage(m) {
        const hint = document.getElementById('empty-hint');
        if (hint) hint.remove();

        const row = document.createElement('div');
        row.className = 'msg' + (m.senderId === myId ? ' mine' : '');

        const bubble = document.createElement('div');
        bubble.className = 'bubble';

        const content = document.createElement('div');
        content.textContent = m.content;

        const time = document.createElement('div');
        time.className = 'time';
        time.textContent = m.sentAt;

        bubble.appendChild(content);
        bubble.appendChild(time);
        row.appendChild(bubble);
        box.appendChild(row);
        scrollToBottom();
    }

    function connect() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // tắt log

        stompClient.connect({}, function () {
            setStatus('Trực tuyến', 'success');

            // nhận tin nhắn của mình (cả tin mình gửi và tin người kia gửi)
            stompClient.subscribe('/user/queue/messages', function (frame) {
                const m = JSON.parse(frame.body);
                // chỉ hiện tin thuộc cuộc trò chuyện đang mở
                if (m.courseId === courseId && m.studentId === studentId) {
                    appendMessage(m);
                }
            });

            stompClient.subscribe('/user/queue/errors', function (frame) {
                alert(frame.body);
            });
        }, function () {
            setStatus('Mất kết nối - thử lại...', 'danger');
            setTimeout(connect, 3000); // tự kết nối lại sau 3 giây
        });
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const text = input.value.trim();
        if (!text || !stompClient || !stompClient.connected) {
            return;
        }
        stompClient.send('/app/chat.send', {}, JSON.stringify({
            courseId: courseId,
            studentId: studentId,
            content: text
        }));
        input.value = '';
        input.focus();
    });

    scrollToBottom();
    connect();
})();
