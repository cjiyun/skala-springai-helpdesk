const form = document.querySelector('#chat-form');
const answer = document.querySelector('#answer');
const sources = document.querySelector('#sources');
const status = document.querySelector('#status');
const cancel = document.querySelector('#cancel');
const loadHistory = document.querySelector('#load-history');
const history = document.querySelector('#history');
const historyStatus = document.querySelector('#history-status');
let activeRequest;

form.addEventListener('submit', async event => {
  event.preventDefault();
  activeRequest?.abort();
  activeRequest = new AbortController();
  answer.textContent = '';
  showSources([]);
  status.textContent = '답변을 기다리는 중입니다…';
  cancel.disabled = false;

  try {
    const credentials = `${value('username')}:${value('password')}`;
    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Authorization': `Basic ${base64(credentials)}`,
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream'
      },
      body: JSON.stringify({ sessionId: value('session'), question: value('question') }),
      signal: activeRequest.signal
    });
    if (!response.ok || !response.body) throw new Error(`요청 실패 (${response.status})`);
    await readEvents(response.body);
    status.textContent = '답변이 완료되었습니다.';
  } catch (error) {
    status.textContent = error.name === 'AbortError' ? '요청을 중지했습니다.' : error.message;
  } finally {
    cancel.disabled = true;
    activeRequest = undefined;
  }
});

cancel.addEventListener('click', () => activeRequest?.abort());

loadHistory.addEventListener('click', async () => {
  historyStatus.textContent = '대화 이력을 조회하는 중입니다…';
  try {
    const credentials = `${value('username')}:${value('password')}`;
    const sessionId = encodeURIComponent(value('session'));
    const response = await fetch(`/api/chat/history?sessionId=${sessionId}`, {
      headers: { 'Authorization': `Basic ${base64(credentials)}` }
    });
    if (!response.ok) throw new Error(`이력 조회 실패 (${response.status})`);
    showHistory((await response.json()).history || []);
    historyStatus.textContent = '대화 이력을 조회했습니다.';
  } catch (error) {
    showHistory([]);
    historyStatus.textContent = error.message;
  }
});

async function readEvents(body) {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
    const events = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop();
    events.forEach(handleEvent);
    if (done) {
      if (buffer.trim()) handleEvent(buffer);
      return;
    }
  }
}

function handleEvent(block) {
  let name = '';
  const data = [];
  block.split(/\r?\n/).forEach(line => {
    if (line.startsWith('event:')) name = line.slice(6).trim();
    if (line.startsWith('data:')) data.push(line.slice(5));
  });
  const payload = data.join('\n');
  if (name === 'token') answer.textContent += payload;
  if (name === 'sources') showSources(JSON.parse(payload || '[]'));
}

function showSources(items) {
  sources.replaceChildren();
  if (!items.length) {
    const item = document.createElement('li');
    item.textContent = '확인된 출처가 없습니다.';
    sources.append(item);
    return;
  }
  items.forEach(source => {
    const item = document.createElement('li');
    item.textContent = `${source.document} (${source.version})`;
    sources.append(item);
  });
}

function showHistory(messages) {
  history.replaceChildren();
  const items = messages.length ? messages : ['조회된 대화가 없습니다.'];
  items.forEach(message => {
    const item = document.createElement('li');
    item.textContent = message;
    history.append(item);
  });
}

function value(id) { return document.querySelector(`#${id}`).value.trim(); }
function base64(text) {
  const bytes = new TextEncoder().encode(text);
  let binary = '';
  bytes.forEach(byte => binary += String.fromCharCode(byte));
  return btoa(binary);
}
