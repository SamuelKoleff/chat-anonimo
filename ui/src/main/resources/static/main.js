let sessionId = 's_' + Math.random().toString(36).substring(2,10);
let userId = 'u_' + Math.random().toString(36).substring(2,8);
let roomId = '';
let stompClient = null;
let eventSource = null;

const connectBtn = document.getElementById('connectBtn');
const joinQueueBtn = document.getElementById('joinQueueBtn');
const disconnectBtn = document.getElementById('disconnectBtn');
const statusDiv = document.getElementById('status');
const roomSpan = document.getElementById('roomId');
const connectChatBtn = document.getElementById('connectChatBtn');
const disconnectChatBtn = document.getElementById('disconnectChatBtn');
const chatDiv = document.getElementById('chat');
const messagesDiv = document.getElementById('messages');
const messageInput = document.getElementById('messageInput');
const sendBtn = document.getElementById('sendBtn');

function updateStatus(statusText, className) {
    statusDiv.textContent = statusText;
    statusDiv.className = 'status ' + className;
}

async function setSessionStatus(status) {
    await fetch(`http://localhost:8080/user-session-service/sessions/${sessionId}/status?status=${status}`, { method: 'PUT' });
}

function subscribeToSessionUpdates() {
    eventSource = new EventSource(`http://localhost:8080/user-session-service/sessions/${sessionId}/stream`);

    eventSource.onmessage = (event) => {
        const data = JSON.parse(event.data);
        console.log('Session update:', data);

        if(data.matched && data.roomId){
            roomId = data.roomId;
            roomSpan.textContent = roomId;
            updateStatus('Emparejado', 'matched');
            connectChatBtn.disabled = false;
        } else if(data.available){
            updateStatus('En cola', 'waiting');
        } else if(data.up){
            updateStatus('Registrado (UP)', 'up');
        } else {
            updateStatus('Desconectado', 'disconnected');
        }
    };

    eventSource.onerror = (error) => {
        console.error('EventSource error:', error);
        updateStatus('Error de conexión', 'disconnected');
    };
}

async function registerSession() {
    await fetch(`http://localhost:8080/user-session-service/sessions`, {
        method: 'POST',
        headers:{'Content-Type':'application/json'},
        body: JSON.stringify({ sessionId, userId })
    });

    updateStatus('Registrado (UP)', 'up');
    joinQueueBtn.disabled = false;
    disconnectBtn.disabled = false;

    subscribeToSessionUpdates();
}

function connectChat() {
    if(!roomId) {
        alert('No hay roomId');
        return;
    }
    const socket = new SockJS('http://localhost:8080/chat-service/websocket');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, frame =>{
        chatDiv.style.display='block';
        connectChatBtn.style.display='none';
        disconnectChatBtn.style.display='inline-block';

        stompClient.subscribe(`/topic/match/${roomId}`, msg=>{
            const m = JSON.parse(msg.body);
            const div = document.createElement('div');
            div.textContent = `${m.sender}: ${m.content}`;
            messagesDiv.appendChild(div);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        });
    });
}

function disconnectChat() {
    if(stompClient) stompClient.disconnect();
    chatDiv.style.display='none';
    connectChatBtn.style.display='inline-block';
    disconnectChatBtn.style.display='none';
}

function sendMessage() {
    const content = messageInput.value.trim();
    if(!content || !stompClient) return;

    stompClient.send(`/app/match/${roomId}`, {}, JSON.stringify({sender:userId,content}));
    messageInput.value='';
}

connectBtn.addEventListener('click', registerSession);
joinQueueBtn.addEventListener('click', ()=>setSessionStatus('AVAILABLE'));
disconnectBtn.addEventListener('click', () => {
    setSessionStatus('DISCONNECTED');
    if(eventSource) {
        eventSource.close();
        eventSource = null;
    }
});
connectChatBtn.addEventListener('click', connectChat);
disconnectChatBtn.addEventListener('click', disconnectChat);
sendBtn.addEventListener('click', sendMessage);
messageInput.addEventListener('keypress', e=>{ if(e.key==='Enter') sendMessage() });