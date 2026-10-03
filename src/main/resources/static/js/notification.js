// Thông báo realtime: kết nối WebSocket trên mọi trang khi đã đăng nhập
// Server đẩy về /user/queue/notifications -> tăng số trên chuông + thêm vào dropdown + hiện toast
(function () {
    const badge = document.getElementById('bell-badge');
    const list = document.getElementById('notif-list');
    if (!badge || !list || !window.SockJS || !window.Stomp) return;

    function bumpBadge() {
        const n = (parseInt(badge.textContent, 10) || 0) + 1;
        badge.textContent = n;
        badge.classList.remove('d-none');
    }

    // dùng textContent để tránh bị chèn HTML (XSS)
    function addItem(n) {
        const empty = document.getElementById('notif-empty');
        if (empty) empty.remove();

        const a = document.createElement('a');
        a.className = 'dropdown-item notif-item bg-primary-subtle py-2';
        a.href = '/notifications/' + n.id + '/go';

        const title = document.createElement('div');
        title.className = 'fw-semibold small';
        title.textContent = n.title;
        const content = document.createElement('div');
        content.className = 'small text-wrap';
        content.textContent = n.content || '';
        const time = document.createElement('div');
        time.className = 'small text-muted';
        time.textContent = n.createdAt;

        a.append(title, content, time);
        list.prepend(a);
        while (list.children.length > 10) list.lastElementChild.remove();
    }

    function showToast(n) {
        let box = document.getElementById('toast-box');
        if (!box) {
            box = document.createElement('div');
            box.id = 'toast-box';
            box.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            document.body.appendChild(box);
        }
        const toast = document.createElement('div');
        toast.className = 'toast show';
        toast.setAttribute('role', 'alert');

        const head = document.createElement('div');
        head.className = 'toast-header';
        const strong = document.createElement('strong');
        strong.className = 'me-auto';
        strong.textContent = n.title;
        const close = document.createElement('button');
        close.type = 'button';
        close.className = 'btn-close';
        close.addEventListener('click', () => toast.remove());
        head.append(strong, close);

        const body = document.createElement('a');
        body.className = 'toast-body d-block text-decoration-none text-dark';
        body.href = '/notifications/' + n.id + '/go';
        body.textContent = n.content || '';

        toast.append(head, body);
        box.appendChild(toast);
        setTimeout(() => toast.remove(), 6000);
    }

    function connect() {
        const client = Stomp.over(new SockJS('/ws'));
        client.debug = null;
        client.connect({}, function () {
            client.subscribe('/user/queue/notifications', function (frame) {
                const n = JSON.parse(frame.body);
                bumpBadge();
                addItem(n);
                showToast(n);
            });
        }, function () {
            // mất kết nối thì thử lại sau 5 giây
            setTimeout(connect, 5000);
        });
    }

    connect();
})();
