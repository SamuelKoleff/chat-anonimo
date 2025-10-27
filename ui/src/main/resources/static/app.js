// Variables globales
let stompClient = null;
let userId = '';
let sessionId = '';
let roomId = '';
let statusCheckInterval = null;

// Elementos del DOM
const registerBtn = document.getElementById('registerBtn');
const joinQueueBtn = document.getElementById('joinQueueBtn');
const checkStatusBtn = document.getElementById('checkStatusBtn');
const disconnectUserBtn = document.getElementById('disconnectUserBtn');
const connectChatBtn = document.getElementById('connectChatBtn');
const disconnectChatBtn = document.getElementById('disconnectChatBtn');
const sendBtn = document.getElementById('sendBtn');

const userIdInput = document.getElementById('userId');
const sessionIdInput = document.getElementById('sessionId');
const chatRoomIdInput = document.getElementById('chatRoomId');
const messageInput = document.getElementById('messageInput');

const messagesContainer = document.getElementById('messages');
const userStatusDiv = document.getElementById('userStatus');
const chatStatusDiv = document.getElementById('chatStatus');

const infoSessionId = document.getElementById('infoSessionId');
const infoUserId = document.getElementById('infoUserId');
const infoStatus = document.getElementById('infoStatus');
const infoRoomId = document.getElementById('infoRoomId');

const userLog = document.getElementById('userLog');

// Funciones de utilidad
function addLog(message, type = 'info') {
    const logEntry = document.createElement('div');
    logEntry.className = `log-entry log-${type}`;
    logEntry.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
    userLog.appendChild(logEntry);
    userLog.scrollTop = userLog.scrollHeight;
}

function updateUserInfo(sessionData) {
    infoSessionId.textContent = sessionData.sessionId || '-';
    infoUserId.textContent = sessionData.userId || '-';
    infoStatus.textContent = sessionData.status || '-';
    infoRoomId.textContent = sessionData.roomId || '-';
    
    // Actualizar estado visual
    if (sessionData.status === 'MATCHED' && sessionData.roomId) {
        userStatusDiv.textContent = 'Emparejado - Room ID: ' + sessionData.roomId;
        userStatusDiv.className = 'status connected';
        chatRoomIdInput.value = sessionData.roomId;
        connectChatBtn.disabled = false;
    } else if (sessionData.status === 'AVAILABLE') {
        userStatusDiv.textContent = 'En cola de espera';
        userStatusDiv.className = 'status waiting';
    } else if (sessionData.status === 'UP') {
        userStatusDiv.textContent = 'Registrado';
        userStatusDiv.className = 'status waiting';
    } else {
        userStatusDiv.textContent = 'No registrado';
        userStatusDiv.className = 'status disconnected';
    }
}

// Funciones de API REST
async function registerUser() {
    userId = userIdInput.value.trim();
    sessionId = sessionIdInput.value.trim();
    
    if (!userId || !sessionId) {
        alert('Por favor ingresa tanto el ID de usuario como el ID de sesión');
        return;
    }
    
    try {
        const response = await fetch('http://localhost:8080/user-session-service/', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                sessionId: sessionId,
                userId: userId
            })
        });
        
        if (response.ok) {
            addLog('Usuario registrado exitosamente', 'success');
            joinQueueBtn.disabled = false;
            checkStatusBtn.disabled = false;
            disconnectUserBtn.disabled = false;
            
            // Obtener información inicial
            checkUserStatus();
        } else {
            addLog('Error al registrar usuario: ' + response.status, 'error');
        }
    } catch (error) {
        addLog('Error de conexión: ' + error.message, 'error');
    }
}

async function joinQueue() {
    try {
        const response = await fetch(`http://localhost:8080/user-session-service/${sessionId}/status?status=AVAILABLE`, {
            method: 'PUT'
        });
        
        if (response.ok) {
            addLog('Usuario agregado a la cola de espera', 'success');
            
            // Iniciar verificación periódica del estado
            if (statusCheckInterval) {
                clearInterval(statusCheckInterval);
            }
            statusCheckInterval = setInterval(checkUserStatus, 3000); // Verificar cada 3 segundos
        } else {
            addLog('Error al unirse a la cola: ' + response.status, 'error');
        }
    } catch (error) {
        addLog('Error de conexión: ' + error.message, 'error');
    }
}

async function checkUserStatus() {
    try {
        const response = await fetch(`http://localhost:8080/user-session-service/${sessionId}`);
        
        if (response.ok) {
            const sessionData = await response.json();
            addLog(`Estado verificado: ${sessionData.status}`, 'info');
            updateUserInfo(sessionData);
            
            // Si está emparejado, detener la verificación periódica
            if (sessionData.status === 'MATCHED' && sessionData.roomId) {
                if (statusCheckInterval) {
                    clearInterval(statusCheckInterval);
                    statusCheckInterval = null;
                }
                roomId = sessionData.roomId;
                addLog(`¡Emparejado! Room ID: ${roomId}`, 'success');
            }
        } else {
            addLog('Error al verificar estado: ' + response.status, 'error');
        }
    } catch (error) {
        addLog('Error de conexión: ' + error.message, 'error');
    }
}

async function disconnectUser() {
    try {
        const response = await fetch(`http://localhost:8080/user-session-service/${sessionId}/status?status=DISCONNECTED`, {
            method: 'PUT'
        });
        
        if (response.ok) {
            addLog('Usuario desconectado', 'info');
            
            // Limpiar estado
            if (statusCheckInterval) {
                clearInterval(statusCheckInterval);
                statusCheckInterval = null;
            }
            
            updateUserInfo({});
            userStatusDiv.textContent = 'Desconectado';
            userStatusDiv.className = 'status disconnected';
            
            // Deshabilitar botones
            joinQueueBtn.disabled = true;
            checkStatusBtn.disabled = true;
            disconnectUserBtn.disabled = true;
            connectChatBtn.disabled = true;
            
            // Desconectar chat si está conectado
            if (stompClient && stompClient.connected) {
                disconnectChat();
            }
        } else {
            addLog('Error al desconectar usuario: ' + response.status, 'error');
        }
    } catch (error) {
        addLog('Error de conexión: ' + error.message, 'error');
    }
}

// Funciones de WebSocket para el chat
function connectChat() {
    roomId = chatRoomIdInput.value.trim();
    
    if (!roomId) {
        alert('No hay un Room ID disponible. Primero debes ser emparejado.');
        return;
    }
    
    // URL base del WebSocket
    const socketUrl = 'http://localhost:8080/chat-service/websocket';
    
    // Crear cliente SockJS
    const socket = new SockJS(socketUrl);
    stompClient = Stomp.over(socket);
    
    // Conectar al servidor
    stompClient.connect({}, function(frame) {
        // Actualizar estado
        setChatConnected(true);
        
        // Suscribirse al topic de la sala
        stompClient.subscribe(`/topic/match/${roomId}`, function(message) {
            showMessage(JSON.parse(message.body));
        });
        
        addLog('Chat conectado exitosamente', 'success');
        console.log('Conectado: ' + frame);
    }, function(error) {
        console.error('Error de conexión: ' + error);
        setChatConnected(false);
        addLog('Error al conectar el chat: ' + error, 'error');
    });
}

function disconnectChat() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setChatConnected(false);
    addLog('Chat desconectado', 'info');
    console.log('Desconectado');
}

function sendMessage() {
    const content = messageInput.value.trim();
    
    if (content && stompClient) {
        const message = {
            sender: userId,
            content: content
        };
        
        // Enviar mensaje al servidor
        stompClient.send(`/app/match/${roomId}`, {}, JSON.stringify(message));
        
        // Limpiar campo de entrada
        messageInput.value = '';
    }
}

function showMessage(message) {
    const messageElement = document.createElement('div');
    messageElement.classList.add('message');
    
    // Determinar si es nuestro mensaje o de otro usuario
    if (message.sender === userId) {
        messageElement.classList.add('own-message');
    } else {
        messageElement.classList.add('other-message');
    }
    
    // Crear contenido del mensaje
    const senderElement = document.createElement('div');
    senderElement.classList.add('message-sender');
    senderElement.textContent = message.sender;
    
    const contentElement = document.createElement('div');
    contentElement.classList.add('message-content');
    contentElement.textContent = message.content;
    
    const timestampElement = document.createElement('div');
    timestampElement.classList.add('timestamp');
    timestampElement.textContent = new Date().toLocaleTimeString();
    
    // Agregar elementos al mensaje
    messageElement.appendChild(senderElement);
    messageElement.appendChild(contentElement);
    messageElement.appendChild(timestampElement);
    
    // Agregar mensaje al contenedor
    messagesContainer.appendChild(messageElement);
    
    // Desplazar hacia abajo para ver el último mensaje
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

function setChatConnected(connected) {
    if (connected) {
        chatStatusDiv.textContent = 'Chat Conectado';
        chatStatusDiv.className = 'status connected';
        connectChatBtn.style.display = 'none';
        disconnectChatBtn.style.display = 'inline-block';
        messageInput.disabled = false;
        sendBtn.disabled = false;
    } else {
        chatStatusDiv.textContent = 'Chat Desconectado';
        chatStatusDiv.className = 'status disconnected';
        connectChatBtn.style.display = 'inline-block';
        disconnectChatBtn.style.display = 'none';
        messageInput.disabled = true;
        sendBtn.disabled = true;
    }
}

// Event Listeners
registerBtn.addEventListener('click', registerUser);
joinQueueBtn.addEventListener('click', joinQueue);
checkStatusBtn.addEventListener('click', checkUserStatus);
disconnectUserBtn.addEventListener('click', disconnectUser);
connectChatBtn.addEventListener('click', connectChat);
disconnectChatBtn.addEventListener('click', disconnectChat);
sendBtn.addEventListener('click', sendMessage);

// Permitir enviar mensaje con Enter
messageInput.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        sendMessage();
    }
});

// Inicialización
addLog('Sistema inicializado. Comienza registrando un usuario.', 'info');