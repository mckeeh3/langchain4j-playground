const chatForm = document.getElementById('chat-form');
const chatMessages = document.getElementById('chat-messages');
const chatInput = document.getElementById('chat-input');

chatForm.addEventListener('submit', sendMessage);

function sendMessage(event) {
  event.preventDefault();
  const message = chatInput.value.trim();
  if (message !== '') {
    displayMessage('You', message);
    chatInput.value = '';

    fetch('/api/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ message: message }),
    })
      .then((response) => response.json())
      .then((data) => {
        displayMessage('Assistant', data.response);
      })
      .catch((error) => {
        console.error('Error:', error);
      });
  }
}

function displayMessage(sender, message) {
  const messageElement = document.createElement('div');
  messageElement.innerHTML = `<strong>${sender}:</strong> ${message}`;
  chatMessages.appendChild(messageElement);
  chatMessages.scrollTop = chatMessages.scrollHeight;
}
