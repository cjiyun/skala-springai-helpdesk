const form = document.querySelector('#chat-form');
const status = document.querySelector('#status');
const cancel = document.querySelector('#cancel');
const history = document.querySelector('#history');
const authStatus = document.querySelector('#auth-status');
const sessionList = document.querySelector('#session-list');
const sessionTitle = document.querySelector('#session-title');
const authState = document.querySelector('#auth-state');
const sessionsKey = 'helpdesk.sessions';
let activeRequest;
let streamingAnswer;
let streamingSources;

const initialSession = storedSessions()[0] || createSessionId();
document.querySelector('#session').value = initialSession;
sessionTitle.textContent = initialSession;
rememberSession(initialSession);
loadHistory();

form.addEventListener('submit', async event => {
  event.preventDefault();
  activeRequest?.abort();
  activeRequest = new AbortController();
  const question = value('question');
  const sessionId = value('session');
  rememberSession(sessionId);
  sessionTitle.textContent = sessionId;
  removeEmptyState();
  appendMessage({ role: 'USER', content: question });
  ({ content: streamingAnswer, sources: streamingSources } = appendStreamingAnswer());
  status.textContent = '답변을 생성하는 중입니다…';
  cancel.disabled = false;

  try {
    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream'
      },
      body: JSON.stringify({ sessionId, question }),
      signal: activeRequest.signal
    });
    if (!response.ok || !response.body) {
      if (response.status === 401) markLoggedOut();
      throw new Error(response.status === 401 ? '로그인이 필요합니다.' : `요청 실패 (${response.status})`);
    }
    await readEvents(response.body);
    status.textContent = '답변이 완료되었습니다.';
    document.querySelector('#question').value = '';
  } catch (error) {
    status.textContent = error.name === 'AbortError' ? '요청을 중지했습니다.' : error.message;
  } finally {
    cancel.disabled = true;
    activeRequest = undefined;
  }
});

cancel.addEventListener('click', () => activeRequest?.abort());
document.querySelector('#question').addEventListener('keydown', event => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    form.requestSubmit();
  }
});
document.querySelector('#load-history').addEventListener('click', loadHistory);
document.querySelector('#login').addEventListener('click', login);
document.querySelector('#logout').addEventListener('click', logout);
document.querySelector('#new-session').addEventListener('click', () => {
  const sessionId = createSessionId();
  document.querySelector('#session').value = sessionId;
  rememberSession(sessionId);
  sessionTitle.textContent = '새 상담';
  resetConversation('새 대화를 시작했습니다.');
});
document.querySelector('#delete-session').addEventListener('click', async () => {
  const sessionId = value('session');
  if (!window.confirm(`'${sessionId}' 대화를 삭제할까요?`)) return;
  try {
    const response = await fetch(`/api/chat/history?sessionId=${encodeURIComponent(sessionId)}`, {
      method: 'DELETE'
    });
    if (!response.ok) throw new Error(`대화 삭제 실패 (${response.status})`);
    forgetSession(sessionId);
    const nextSession = storedSessions()[0] || createSessionId();
    document.querySelector('#session').value = nextSession;
    rememberSession(nextSession);
    sessionTitle.textContent = nextSession;
    resetConversation('대화를 삭제했습니다.');
  } catch (error) {
    status.textContent = error.message;
  }
});

async function loadHistory() {
  status.textContent = '대화 이력을 조회하는 중입니다…';
  try {
    const sessionId = value('session');
    const response = await fetch(`/api/chat/history?sessionId=${encodeURIComponent(sessionId)}`);
    if (!response.ok) throw new Error(response.status === 401
      ? '로그인이 필요합니다.'
      : `이력 조회 실패 (${response.status})`);
    markLoggedIn();
    showHistory((await response.json()).history || []);
    rememberSession(sessionId);
    sessionTitle.textContent = sessionId;
    status.textContent = '대화 이력을 조회했습니다.';
  } catch (error) {
    if (error.message === '로그인이 필요합니다.') markLoggedOut();
    showHistory([]);
    status.textContent = error.message;
  }
}

async function login() {
  authStatus.textContent = '로그인하는 중입니다…';
  const body = new URLSearchParams({ username: value('username'), password: value('password') });
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body
  });
  if (!response.ok) {
    markLoggedOut();
    authStatus.textContent = '사용자명 또는 비밀번호를 확인해 주세요.';
    return;
  }
  document.querySelector('#password').value = '';
  markLoggedIn();
  document.querySelector('.settings').open = false;
  await loadHistory();
}

async function logout() {
  await fetch('/api/auth/logout', { method: 'POST' });
  markLoggedOut();
  resetConversation('로그아웃했습니다.');
}

function markLoggedIn() {
  authState.classList.remove('pending');
  authState.lastChild.textContent = '로그인됨';
  authStatus.textContent = '로그인되었습니다.';
}

function markLoggedOut() {
  authState.classList.add('pending');
  authState.lastChild.textContent = '로그인 필요';
  authStatus.textContent = '로그인이 필요합니다.';
}

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
  if (name === 'token' && streamingAnswer) streamingAnswer.textContent += payload;
  if (name === 'sources' && streamingSources) {
    showSources(streamingSources, JSON.parse(payload || '[]'));
  }
}

function showSources(target, items) {
  target.replaceChildren();
  const values = items.length ? items : [{ document: '확인된 출처가 없습니다.', version: '' }];
  values.forEach(source => {
    const item = document.createElement('li');
    item.textContent = source.version ? `${source.document} · v${source.version}` : source.document;
    target.append(item);
  });
}

function showHistory(messages) {
  history.replaceChildren();
  streamingAnswer = undefined;
  streamingSources = undefined;
  if (!messages.length) {
    history.append(emptyState('저장된 대화가 없습니다.'));
    return;
  }
  messages.forEach(appendMessage);
  history.scrollTop = history.scrollHeight;
}

function appendStreamingAnswer() {
  const bubble = document.createElement('div');
  bubble.className = 'message assistant';
  const label = document.createElement('div');
  label.className = 'message-label';
  label.textContent = 'HelpDesk AI';
  const content = document.createElement('div');
  const sourceBox = document.createElement('div');
  sourceBox.className = 'source-box';
  const sourceLabel = document.createElement('strong');
  sourceLabel.textContent = '출처';
  const sourceList = document.createElement('ul');
  showSources(sourceList, []);
  sourceBox.append(sourceLabel, sourceList);
  bubble.append(label, content, sourceBox);
  history.append(bubble);
  history.scrollTop = history.scrollHeight;
  return { content, sources: sourceList };
}

function appendMessage(message) {
  if (message.role === 'TOOL') {
    const card = document.createElement('div');
    card.className = 'tool-card';
    card.textContent = `⚙ ${toolLabel(message.toolName)} · ${message.status}`;
    history.append(card);
    return;
  }
  const bubble = document.createElement('div');
  bubble.className = `message ${message.role === 'USER' ? 'user' : 'assistant'}`;
  const label = document.createElement('div');
  label.className = 'message-label';
  label.textContent = message.role === 'USER' ? '나' : 'HelpDesk AI';
  const content = document.createElement('div');
  content.textContent = message.content;
  bubble.append(label, content);
  history.append(bubble);
}

function toolLabel(name) {
  return ({ getOrder: '주문 조회', getTicketStatus: '티켓 조회',
    requestRefund: '환불 접수', requestExchange: '교환 접수' })[name] || name || '도구 실행';
}

function rememberSession(sessionId) {
  if (!sessionId) return;
  const sessions = [sessionId, ...storedSessions().filter(item => item !== sessionId)].slice(0, 8);
  localStorage.setItem(sessionsKey, JSON.stringify(sessions));
  renderSessions(sessions);
}

function forgetSession(sessionId) {
  const sessions = storedSessions().filter(item => item !== sessionId);
  localStorage.setItem(sessionsKey, JSON.stringify(sessions));
  renderSessions(sessions);
}

function renderSessions(sessions = storedSessions()) {
  sessionList.replaceChildren();
  sessions.forEach(sessionId => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'session-link';
    button.textContent = sessionId;
    button.addEventListener('click', () => {
      document.querySelector('#session').value = sessionId;
      sessionTitle.textContent = sessionId;
      loadHistory();
    });
    sessionList.append(button);
  });
}

function storedSessions() {
  try { return JSON.parse(localStorage.getItem(sessionsKey) || '[]'); }
  catch { return []; }
}

function resetConversation(message) {
  history.replaceChildren(emptyState(message));
  streamingAnswer = undefined;
  streamingSources = undefined;
  status.textContent = message;
}

function emptyState(text) {
  const element = document.createElement('div');
  element.className = 'empty-state';
  element.textContent = text;
  return element;
}

function removeEmptyState() { history.querySelector('.empty-state')?.remove(); }
function createSessionId() {
  return `session-${new Date().toISOString().replace(/\D/g, '').slice(0, 17)}`;
}
function value(id) { return document.querySelector(`#${id}`).value.trim(); }
