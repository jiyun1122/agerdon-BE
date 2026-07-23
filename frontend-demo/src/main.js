import "./styles.css";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "";
const TOKEN_KEY = "agerdon-demo-token";
const THRESHOLDS = [
  { seconds: 1800, label: "골든타임 30분 전", tone: "safe" },
  { seconds: 900, label: "골든타임 15분 전", tone: "safe" },
  { seconds: 300, label: "골든타임 5분 전", tone: "danger" },
  { seconds: 0, label: "골든타임 정각", tone: "danger" }
];

const state = {
  token: localStorage.getItem(TOKEN_KEY),
  trip: null,
  loading: false,
  error: "",
  notice: "",
  alarm: null,
  serverDeltaMs: 0,
  testOffsetMs: 0,
  previousRemaining: null,
  firedThresholds: new Set()
};

let audioContext;
const app = document.querySelector("#app");

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function currency(value) {
  if (value == null) return "-";
  return `${Number(value).toLocaleString("ko-KR")}원`;
}

function parseDate(value) {
  return value ? new Date(value) : null;
}

function formatClock(value) {
  const date = value instanceof Date ? value : parseDate(value);
  if (!date || Number.isNaN(date.getTime())) return "-";
  return new Intl.DateTimeFormat("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false
  }).format(date);
}

function formatDateTime(value) {
  const date = value instanceof Date ? value : parseDate(value);
  if (!date || Number.isNaN(date.getTime())) return "-";
  return new Intl.DateTimeFormat("ko-KR", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false
  }).format(date);
}

function formatDuration(totalSeconds) {
  if (totalSeconds == null) return "--:--";
  const absolute = Math.abs(Math.trunc(totalSeconds));
  const hours = Math.floor(absolute / 3600);
  const minutes = Math.floor((absolute % 3600) / 60);
  const seconds = absolute % 60;
  const clock = hours > 0
    ? `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`
    : `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
  return totalSeconds < 0 ? `+${clock}` : clock;
}

function simulatedNowMs() {
  return Date.now() + state.serverDeltaMs + state.testOffsetMs;
}

function remainingSeconds() {
  const goldenTime = parseDate(state.trip?.timer?.goldenTime);
  if (!goldenTime) return null;
  return Math.floor((goldenTime.getTime() - simulatedNowMs()) / 1000);
}

function timerViewState() {
  const remaining = remainingSeconds();
  if (remaining == null) return "UNAVAILABLE";
  return remaining >= 0 ? "RUNNING" : "EXPIRED";
}

async function api(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(body.message || "요청 처리에 실패했습니다.");
    error.code = body.code;
    throw error;
  }
  return body;
}

function syncTrip(trip) {
  state.trip = trip;
  state.testOffsetMs = 0;
  state.previousRemaining = null;
  state.firedThresholds.clear();
  const serverTime = parseDate(trip?.timer?.serverTime);
  state.serverDeltaMs = serverTime ? serverTime.getTime() - Date.now() : 0;
}

function unlockAudio() {
  if (!audioContext) {
    audioContext = new AudioContext();
  }
  if (audioContext.state === "suspended") {
    audioContext.resume();
  }
}

function playAlarm(tone = "safe") {
  unlockAudio();
  const frequency = tone === "danger" ? 880 : 660;
  [0, 0.24, 0.48].forEach((delay, index) => {
    const oscillator = audioContext.createOscillator();
    const gain = audioContext.createGain();
    oscillator.type = index === 2 ? "square" : "sine";
    oscillator.frequency.value = frequency + index * 80;
    gain.gain.setValueAtTime(0.0001, audioContext.currentTime + delay);
    gain.gain.exponentialRampToValueAtTime(0.18, audioContext.currentTime + delay + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.0001, audioContext.currentTime + delay + 0.18);
    oscillator.connect(gain);
    gain.connect(audioContext.destination);
    oscillator.start(audioContext.currentTime + delay);
    oscillator.stop(audioContext.currentTime + delay + 0.2);
  });
}

function openAlarm(threshold) {
  state.firedThresholds.add(threshold.seconds);
  state.alarm = {
    title: threshold.label,
    message: threshold.seconds === 0
      ? "지금 출발해야 합니다. 막차 탑승을 위해 바로 이동하세요."
      : `현재 위치에서 출발할 시간까지 ${threshold.seconds / 60}분 남았습니다.`,
    tone: threshold.tone
  };
  playAlarm(threshold.tone);
}

function detectThreshold(previous, current) {
  if (previous == null || current == null || current >= previous) return;
  const crossed = THRESHOLDS
    .filter((item) => previous > item.seconds && current <= item.seconds)
    .filter((item) => !state.firedThresholds.has(item.seconds))
    .sort((a, b) => Math.abs(current - a.seconds) - Math.abs(current - b.seconds));
  if (crossed.length > 0) {
    openAlarm(crossed[0]);
  }
}

function routeIcon(type) {
  return {
    SUBWAY: "M",
    BUS: "B",
    NBUS: "N",
    TAXI: "T"
  }[type] || "R";
}

function routeCard(route, alternative = false) {
  return `
    <article class="route-card ${route.recommended ? "recommended" : ""} ${alternative ? "alternative" : ""}">
      <div class="route-icon type-${escapeHtml(route.type)}">${routeIcon(route.type)}</div>
      <div class="route-copy">
        <div class="route-title-row">
          <strong>${escapeHtml(route.name)}</strong>
          ${route.recommended ? '<span class="route-badge">추천</span>' : ""}
        </div>
        <p>${escapeHtml(route.guide)}</p>
        <div class="route-meta">
          <span>총 ${escapeHtml(route.totalMinutes)}분</span>
          <span>도보 ${escapeHtml(route.walkMinutes)}분</span>
          <span>${currency(route.fare)}</span>
        </div>
        ${route.scheduledAt ? `
          <div class="route-time">
            ${route.type === "NBUS" ? "정류장 출발" : "막차"} ${formatClock(route.scheduledAt)}
            <b>· 현위치 출발 ${formatClock(route.departureDeadline)}</b>
          </div>
        ` : ""}
      </div>
    </article>
  `;
}

function statusMessages() {
  return `
    ${state.error ? `<div class="message error-message">${escapeHtml(state.error)}</div>` : ""}
    ${state.notice ? `<div class="message notice-message">${escapeHtml(state.notice)}</div>` : ""}
  `;
}

function renderAuth() {
  app.innerHTML = `
    <main class="auth-page">
      <section class="auth-card">
        <div class="brand-mark">막</div>
        <p class="eyebrow">HONGIK LAST TRANSIT</p>
        <h1>막차 알리미</h1>
        <p class="auth-description">백엔드 JWT 인증과 Trip API를 직접 호출하는 로컬 연동 데모입니다.</p>
        ${statusMessages()}
        <form id="auth-form" class="stack-form">
          <label>이메일<input name="email" type="email" value="demo@agerdon.local" required /></label>
          <label>비밀번호<input name="password" type="password" value="Demo1234!" required /></label>
          <label>닉네임<input name="nickname" value="막차테스터" required /></label>
          <button class="primary-button" type="submit" data-action="login">로그인</button>
          <button class="secondary-button" type="button" data-action="signup">회원가입 후 로그인</button>
        </form>
        <p class="helper">H2는 서버 재시작 시 초기화되므로 처음에는 “회원가입 후 로그인”을 누르세요.</p>
      </section>
    </main>
  `;

  const form = document.querySelector("#auth-form");
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    await authenticate(new FormData(form), false);
  });
  document.querySelector('[data-action="signup"]').addEventListener("click", async () => {
    await authenticate(new FormData(form), true);
  });
}

async function authenticate(formData, signUpFirst) {
  state.loading = true;
  state.error = "";
  state.notice = "";
  render();
  const credentials = {
    email: formData.get("email"),
    password: formData.get("password")
  };
  try {
    if (signUpFirst) {
      try {
        await api("/api/v1/auth/signup", {
          method: "POST",
          body: JSON.stringify({
            ...credentials,
            nickname: formData.get("nickname")
          })
        });
      } catch (error) {
        if (!String(error.message).includes("이미")) throw error;
      }
    }
    const response = await api("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(credentials)
    });
    state.token = response.data.accessToken;
    localStorage.setItem(TOKEN_KEY, state.token);
    state.notice = "로그인했습니다.";
    await loadCurrentTrip();
  } catch (error) {
    state.error = `${error.code ? `[${error.code}] ` : ""}${error.message}`;
  } finally {
    state.loading = false;
    render();
  }
}

function locationFields(prefix, label, values) {
  return `
    <fieldset>
      <legend>${label}</legend>
      <label>이름<input name="${prefix}Name" value="${escapeHtml(values.name)}" required /></label>
      <label>주소<input name="${prefix}Address" value="${escapeHtml(values.address)}" required /></label>
      <div class="coordinate-row">
        <label>위도<input name="${prefix}Latitude" type="number" step="any" value="${values.latitude}" required /></label>
        <label>경도<input name="${prefix}Longitude" type="number" step="any" value="${values.longitude}" required /></label>
      </div>
    </fieldset>
  `;
}

function renderSetup() {
  const origin = {
    name: "홍익대학교 T동",
    address: "서울특별시 마포구 와우산로 94",
    latitude: 37.552568681927504,
    longitude: 126.9247935427622
  };
  const destination = {
    name: "영등포구청역",
    address: "서울특별시 영등포구 당산로",
    latitude: 37.525766,
    longitude: 126.896627
  };

  app.innerHTML = `
    <main class="setup-page">
      <header class="desktop-header">
        <div><span class="brand-dot"></span> 막차 알리미 로컬 데모</div>
        <button class="text-button" data-action="logout">로그아웃</button>
      </header>
      <section class="setup-card">
        <p class="eyebrow">TRIP API TEST</p>
        <h1>오늘의 막차 타이머</h1>
        <p>출발지와 도착지를 확인하고 타이머를 시작하세요.</p>
        ${statusMessages()}
        <form id="trip-form" class="trip-form">
          ${locationFields("origin", "출발지", origin)}
          <div class="swap-mark">↓</div>
          ${locationFields("destination", "도착지", destination)}
          <button class="primary-button large" type="submit">타이머 시작</button>
        </form>
      </section>
    </main>
  `;

  document.querySelector('[data-action="logout"]').addEventListener("click", logout);
  document.querySelector("#trip-form").addEventListener("submit", createTrip);
}

async function createTrip(event) {
  event.preventDefault();
  unlockAudio();
  const data = new FormData(event.currentTarget);
  const location = (prefix) => ({
    name: data.get(`${prefix}Name`),
    address: data.get(`${prefix}Address`),
    latitude: Number(data.get(`${prefix}Latitude`)),
    longitude: Number(data.get(`${prefix}Longitude`))
  });

  state.loading = true;
  state.error = "";
  render();
  try {
    const response = await api("/api/v1/trips", {
      method: "POST",
      body: JSON.stringify({
        origin: location("origin"),
        destination: location("destination")
      })
    });
    syncTrip(response.data);
    state.notice = "여정과 타이머를 시작했습니다.";
  } catch (error) {
    if (error.code === "TRIP-002") {
      await loadCurrentTrip();
      state.notice = "진행 중인 여정을 불러왔습니다.";
    } else {
      state.error = `${error.code ? `[${error.code}] ` : ""}${error.message}`;
    }
  } finally {
    state.loading = false;
    render();
  }
}

function timerRing(remaining, viewState) {
  const danger = remaining != null && remaining <= 300;
  const expired = viewState === "EXPIRED";
  const className = expired || danger ? "danger" : "safe";
  const caption = viewState === "UNAVAILABLE"
    ? "시간 정보 없음"
    : expired
      ? "골든타임 경과"
      : "골든타임까지";
  return `
    <div class="timer-ring ${className}">
      <div>
        <span class="timer-caption">${caption}</span>
        <strong>${formatDuration(remaining)}</strong>
        <span class="golden-label">골든타임 ${formatClock(state.trip.timer.goldenTime)}</span>
      </div>
    </div>
  `;
}

function renderTrip() {
  const trip = state.trip;
  const remaining = remainingSeconds();
  const viewState = timerViewState();
  const expired = viewState === "EXPIRED";
  const regularRoutes = trip.routes.filter((route) => ["SUBWAY", "BUS"].includes(route.type));
  const taxi = trip.routes.find((route) => route.type === "TAXI");
  const nextNightBus = trip.routes
    .filter((route) => route.type === "NBUS")
    .filter((route) => route.departureDeadline
      && new Date(route.departureDeadline).getTime() >= simulatedNowMs())
    .sort((a, b) => new Date(a.departureDeadline) - new Date(b.departureDeadline))[0];
  const alternatives = [taxi, nextNightBus].filter(Boolean);
  const busMissing = !trip.routes.some((route) => ["BUS", "NBUS"].includes(route.type));

  app.innerHTML = `
    <main class="demo-shell">
      <section class="phone">
        <header class="phone-header">
          <div class="phone-time">${formatClock(new Date(simulatedNowMs()))}</div>
          <button class="icon-button" data-action="logout" title="로그아웃">↗</button>
          <div class="location-chip"><span>현위치</span>${escapeHtml(trip.origin.name)}</div>
          <div class="location-chip"><span>도착지</span>${escapeHtml(trip.destination.name)}</div>
        </header>

        <div class="phone-content ${expired ? "expired" : ""}">
          ${statusMessages()}
          <p class="alarm-guide">타이머는 30분, 15분, 5분 전과 정각에 울립니다.</p>
          ${timerRing(remaining, viewState)}
          ${!expired && remaining != null && remaining <= 600 && taxi ? `
            <div class="taxi-warning">놓치면 예상 택시비 <b>${currency(taxi.fare)}</b></div>
          ` : ""}

          ${expired ? `
            <div class="expired-row">
              <span>막차 카운트다운이 끝났습니다.</span>
              <b>다른 방법 보기</b>
            </div>
            <section class="alternatives-panel">
              <div class="section-heading">
                <div><span class="section-kicker">PLAN B</span><h2>심야 이동 방법</h2></div>
              </div>
              ${alternatives.length > 0
                ? alternatives.map((route) => routeCard(route, true)).join("")
                : '<p class="empty-copy">조회된 N버스·택시 경로가 없습니다.</p>'}
              ${!alternatives.some((route) => route.type === "NBUS")
                ? '<p class="api-note">N버스 정보가 없습니다. 실제 모드에서는 버스 API 응답과 목적지 운행 권역이 모두 일치해야 표시됩니다.</p>'
                : ""}
              ${!taxi
                ? '<p class="api-note">택시 정보가 없습니다. 카카오 REST API 키와 일일 호출 할당량을 확인하세요.</p>'
                : ""}
            </section>
          ` : `
            <section class="route-section">
              <div class="section-heading">
                <div><span class="section-kicker">MY ROUTE</span><h2>막차 경로</h2></div>
                <span>${regularRoutes.length}개 후보</span>
              </div>
              ${regularRoutes.map((route) => routeCard(route)).join("")}
              ${busMissing
                ? '<p class="api-note">현재 버스 후보가 없습니다. 버스 API 키·응답 또는 목적지 권역 조건을 확인하세요.</p>'
                : ""}
              <div class="map-placeholder">
                <div class="map-line"></div>
                <span class="map-point start">출발</span>
                <span class="map-point end">역/정류장</span>
                <p>지도 영역 · 경로 API 연결 전 플레이스홀더</p>
              </div>
            </section>
          `}

          <div class="result-actions">
            <button class="success-button" data-result="SUCCESS" ${trip.status ? "disabled" : ""}>탑승 성공</button>
            <button class="missed-button" data-result="MISSED" ${trip.status ? "disabled" : ""}>놓쳤어요</button>
          </div>
          ${trip.status ? `<div class="result-banner ${trip.status.toLowerCase()}">저장된 결과: ${trip.status}</div>` : ""}
        </div>
      </section>

      <aside class="test-panel">
        <span class="dev-badge">DEV TIME CONTROL</span>
        <h2>기다리지 않고 알람 테스트</h2>
        <p>브라우저의 표시 시간만 이동합니다. 서버와 DB 시간은 바뀌지 않으며 새로고침하면 초기화됩니다.</p>
        <div class="time-readout">
          <span>테스트 현재 시각</span>
          <strong>${formatDateTime(new Date(simulatedNowMs()))}</strong>
          <small>실제 시간 대비 ${Math.round(state.testOffsetMs / 60000)}분</small>
        </div>
        <div class="test-grid">
          ${THRESHOLDS.map((item) => `
            <button data-target="${item.seconds}">${item.seconds === 0 ? "정각" : `${item.seconds / 60}분 전`}</button>
          `).join("")}
          <button data-target="-60">1분 경과</button>
          <button data-jump="60">+1분</button>
          <button data-jump="300">+5분</button>
          <button data-jump="600">+10분</button>
          <button data-action="reset-time">실시간 복귀</button>
        </div>
        <div class="api-summary">
          <div><span>Trip ID</span><b>#${trip.tripId}</b></div>
          <div><span>서버 상태</span><b>${escapeHtml(trip.timer.state)}</b></div>
          <div><span>화면 상태</span><b>${viewState}</b></div>
          <div><span>경로 수</span><b>${trip.routes.length}</b></div>
        </div>
        <button class="secondary-button full" data-action="refresh">서버에서 현재 여정 다시 조회</button>
        <button class="text-button danger-text" data-action="cancel-trip">현재 여정 취소</button>
      </aside>

      ${state.alarm ? `
        <div class="alarm-overlay ${state.alarm.tone}">
          <div class="alarm-card">
            <div class="alarm-icon">⏰</div>
            <span>막차 알리미</span>
            <h2>${escapeHtml(state.alarm.title)}</h2>
            <p>${escapeHtml(state.alarm.message)}</p>
            <button class="primary-button" data-action="close-alarm">알람 확인</button>
          </div>
        </div>
      ` : ""}
    </main>
  `;

  bindTripEvents();
}

function bindTripEvents() {
  document.querySelector('[data-action="logout"]').addEventListener("click", logout);
  document.querySelectorAll("[data-result]").forEach((button) => {
    button.addEventListener("click", () => submitResult(button.dataset.result));
  });
  document.querySelectorAll("[data-target]").forEach((button) => {
    button.addEventListener("click", () => setTargetTime(Number(button.dataset.target)));
  });
  document.querySelectorAll("[data-jump]").forEach((button) => {
    button.addEventListener("click", () => jumpTime(Number(button.dataset.jump)));
  });
  document.querySelector('[data-action="reset-time"]').addEventListener("click", resetTestTime);
  document.querySelector('[data-action="refresh"]').addEventListener("click", loadCurrentTrip);
  document.querySelector('[data-action="cancel-trip"]').addEventListener("click", cancelTrip);
  document.querySelector('[data-action="close-alarm"]')?.addEventListener("click", () => {
    state.alarm = null;
    render();
  });
}

function setTargetTime(secondsBefore) {
  unlockAudio();
  const goldenTime = parseDate(state.trip?.timer?.goldenTime);
  if (!goldenTime) {
    state.error = "골든타임이 없어 테스트 시간을 이동할 수 없습니다.";
    render();
    return;
  }
  const previous = remainingSeconds();
  const desiredNow = goldenTime.getTime() - secondsBefore * 1000;
  state.testOffsetMs = desiredNow - (Date.now() + state.serverDeltaMs);
  const current = remainingSeconds();
  detectThreshold(previous, current);
  const exactThreshold = THRESHOLDS.find((item) => item.seconds === secondsBefore);
  if (exactThreshold && !state.firedThresholds.has(exactThreshold.seconds)) {
    openAlarm(exactThreshold);
  }
  state.previousRemaining = current;
  render();
}

function jumpTime(seconds) {
  unlockAudio();
  const previous = remainingSeconds();
  state.testOffsetMs += seconds * 1000;
  const current = remainingSeconds();
  detectThreshold(previous, current);
  state.previousRemaining = current;
  render();
}

function resetTestTime() {
  state.testOffsetMs = 0;
  state.previousRemaining = remainingSeconds();
  state.firedThresholds.clear();
  state.alarm = null;
  render();
}

async function submitResult(status) {
  state.error = "";
  try {
    const response = await api(`/api/v1/trips/${state.trip.tripId}/result`, {
      method: "PATCH",
      body: JSON.stringify({ status })
    });
    state.trip = response.data;
    state.notice = status === "SUCCESS" ? "막차 탑승 성공을 저장했습니다." : "놓친 결과를 저장했습니다.";
    state.alarm = {
      title: status === "SUCCESS" ? "막차 탑승 성공!" : "다음 이동 방법을 확인하세요",
      message: status === "SUCCESS"
        ? "오늘도 안전하게 막차를 지켰습니다."
        : "택시와 N버스 후보를 확인할 수 있습니다.",
      tone: status === "SUCCESS" ? "safe" : "danger"
    };
    playAlarm(status === "SUCCESS" ? "safe" : "danger");
  } catch (error) {
    state.error = `${error.code ? `[${error.code}] ` : ""}${error.message}`;
  }
  render();
}

async function cancelTrip() {
  if (!window.confirm("현재 결과 미입력 여정을 취소할까요?")) return;
  try {
    await api(`/api/v1/trips/${state.trip.tripId}`, { method: "DELETE" });
    state.trip = null;
    state.notice = "여정을 취소했습니다.";
    state.testOffsetMs = 0;
  } catch (error) {
    state.error = `${error.code ? `[${error.code}] ` : ""}${error.message}`;
  }
  render();
}

async function loadCurrentTrip() {
  state.loading = true;
  state.error = "";
  try {
    const response = await api("/api/v1/trips/current");
    if (response.data) {
      syncTrip(response.data);
    } else {
      state.trip = null;
    }
  } catch (error) {
    if (error.code === "AUTH-401") {
      logout();
      return;
    }
    state.error = `${error.code ? `[${error.code}] ` : ""}${error.message}`;
  } finally {
    state.loading = false;
    render();
  }
}

function logout() {
  localStorage.removeItem(TOKEN_KEY);
  state.token = null;
  state.trip = null;
  state.error = "";
  state.notice = "";
  state.testOffsetMs = 0;
  render();
}

function render() {
  if (state.loading) {
    app.innerHTML = `
      <main class="loading-page">
        <div class="loader"></div>
        <p>막차 정보를 불러오는 중입니다.</p>
      </main>
    `;
    return;
  }
  if (!state.token) {
    renderAuth();
  } else if (!state.trip) {
    renderSetup();
  } else {
    renderTrip();
  }
}

setInterval(() => {
  if (!state.trip || !state.trip.timer?.goldenTime) return;
  const current = remainingSeconds();
  detectThreshold(state.previousRemaining, current);
  state.previousRemaining = current;
  render();
}, 1000);

render();
if (state.token) {
  loadCurrentTrip();
}
