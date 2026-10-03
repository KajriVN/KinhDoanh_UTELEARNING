// Widget chatbot tư vấn khóa học: gửi câu hỏi tới POST /api/ai/chat, giữ vài lượt hội thoại gần nhất
(function () {
    const toggle = document.getElementById('bot-toggle');
    const panel = document.getElementById('bot-panel');
    const list = document.getElementById('bot-messages');
    const form = document.getElementById('bot-form');
    const input = document.getElementById('bot-input');
    if (!toggle || !panel || !list || !form || !input) return;

    const history = [];   // [{role: 'user'|'assistant', content}]
    let busy = false;

    toggle.addEventListener('click', function () {
        panel.classList.toggle('d-none');
        if (!panel.classList.contains('d-none')) input.focus();
    });
    document.getElementById('bot-close').addEventListener('click', function () {
        panel.classList.add('d-none');
    });

    // Hiển thị an toàn: chỉ biến "[tên](/courses/...)" thành link, mọi thứ khác là text thường (chống XSS)
    function render(container, text) {
        const linkPattern = /\[([^\]]+)\]\((\/courses[^)\s]*)\)/g;
        text.split('\n').forEach(function (line, i) {
            if (i > 0) container.appendChild(document.createElement('br'));
            let last = 0, m;
            while ((m = linkPattern.exec(line)) !== null) {
                container.appendChild(document.createTextNode(line.slice(last, m.index)));
                const a = document.createElement('a');
                a.href = m[2];
                a.textContent = m[1];
                container.appendChild(a);
                last = m.index + m[0].length;
            }
            container.appendChild(document.createTextNode(line.slice(last)));
        });
    }

    function addMessage(role, text) {
        const row = document.createElement('div');
        row.className = 'bot-msg ' + role;
        const bubble = document.createElement('div');
        bubble.className = 'bot-bubble';
        render(bubble, text);
        row.appendChild(bubble);
        list.appendChild(row);
        list.scrollTop = list.scrollHeight;
        return row;
    }

    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        const text = input.value.trim();
        if (!text || busy) return;
        busy = true;
        input.value = '';
        addMessage('user', text);
        const typing = addMessage('bot', 'Đang suy nghĩ...');

        try {
            const res = await fetch('/api/ai/chat', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({message: text, history: history.slice(-6)})
            });
            const data = await res.json();
            typing.remove();
            if (!res.ok) {
                addMessage('bot', data.error || 'Có lỗi xảy ra, bạn thử lại sau nhé.');
            } else {
                addMessage('bot', data.reply);
                history.push({role: 'user', content: text}, {role: 'assistant', content: data.reply});
            }
        } catch (err) {
            typing.remove();
            addMessage('bot', 'Không kết nối được tới máy chủ, bạn thử lại sau nhé.');
        } finally {
            busy = false;
        }
    });

    addMessage('bot', 'Xin chào! Mình là trợ lý UTELearn. Bạn muốn học gì? Mình sẽ gợi ý khóa học phù hợp.');
})();
