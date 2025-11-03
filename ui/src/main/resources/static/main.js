let sessionId = 's_' + Math.random().toString(36).substring(2,10);
let userId = 'u_' + Math.random().toString(36).substring(2,8);
let roomId = '';
let stompClient = null;
let eventSource = null;

const searchBtn = document.getElementById('searchBtn');
const leaveBtn = document.getElementById('leaveBtn');
const statusDiv = document.getElementById('status');
const roomSpan = document.getElementById('roomId');
const chatDiv = document.getElementById('chat');
const messagesDiv = document.getElementById('messages');
const messageInput = document.getElementById('messageInput');
const sendBtn = document.getElementById('sendBtn');

function updateStatus(text, className) {
    statusDiv.textContent = text;
    statusDiv.className = 'status ' + className;
}

async function setSessionStatus(status) {
    console.log("SET STATUS", status);
    await fetch(`http://localhost:8080/user-session-service/sessions/${sessionId}/status?status=${status}`, {
        method: 'PUT'
    });
}

async function registerSession() {
    console.log("REGISTER");
    await fetch(`http://localhost:8080/user-session-service/sessions`, {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ sessionId, userId })
    });

    updateStatus('Conectado (UP)', 'up');
    searchBtn.disabled = false;

    subscribeToSessionUpdates();
}

function subscribeToSessionUpdates() {
    eventSource = new EventSource(`http://localhost:8080/user-session-service/sessions/${sessionId}/stream`);

    eventSource.onmessage = (event) => {
        const data = JSON.parse(event.data);
        console.log("SSE Update:", data);

        const status = data.status;

        if (status === 'MATCHED' && data.roomId) {
            roomId = data.roomId;
            roomSpan.textContent = roomId;
            updateStatus('Emparejado', 'matched');
            connectChat();  // auto connect
            return;
        }

        if (status === 'AVAILABLE') {
            updateStatus('Buscando…', 'waiting');
            return;
        }

        if (status === 'UP') {
            updateStatus('Conectado', 'up');

            if (stompClient != null) {
                console.log("AUTO-DISCONNECT WebSocket (status changed to UP)");
                leaveChat();
                window.alert('El usuario se ha desconectado');
            }

            return;
        }
    };
}

function connectChat() {
    console.log("WEBSOCKET CONNECT");
    const socket = new SockJS('http://localhost:8080/chat-service/websocket');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, () => {
        chatDiv.style.display = 'block';
        searchBtn.disabled = true;
        leaveBtn.disabled = false;

        stompClient.subscribe(`/topic/match/${roomId}`, msg => {
            const m = JSON.parse(msg.body);
            const div = document.createElement('div');
            div.textContent = `${m.sender}: ${m.content}`;
            messagesDiv.appendChild(div);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        });
    });
}

function leaveChat() {
    if(stompClient) {
        stompClient.disconnect();
        stompClient = null;
    }

    chatDiv.style.display = 'none';
    messagesDiv.innerHTML = '';
    roomSpan.textContent = '-';

    setSessionStatus('UP');
    searchBtn.disabled = false;
    leaveBtn.disabled = true;
}

function sendMessage() {
    const content = messageInput.value.trim();
    if (!content || !stompClient) return;

    stompClient.send(`/app/match/${roomId}`, {}, JSON.stringify({sender: userId, content}));
    messageInput.value = '';
}

searchBtn.addEventListener('click', () => setSessionStatus('AVAILABLE'));
leaveBtn.addEventListener('click', leaveChat);
sendBtn.addEventListener('click', sendMessage);
messageInput.addEventListener('keypress', e => { if(e.key==='Enter') sendMessage() });

registerSession();
