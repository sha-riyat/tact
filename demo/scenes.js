// The whole film as one pure function of time: seek(t) sets every style, so frames render in any order.
const TOTAL = 50;
const clamp = (x, a = 0, b = 1) => Math.min(b, Math.max(a, x));
const prog = (t, a, b) => clamp((t - a) / (b - a));
const E = {
  out3: (x) => 1 - Math.pow(1 - x, 3),
  io3: (x) => (x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2),
  back: (x) => { const c1 = 1.70158, c3 = c1 + 1; return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2); },
  in2: (x) => x * x,
};
const lerp = (a, b, x) => a + (b - a) * x;
const bump = (t, t0, dur, up = 0.1, down = 0.15) =>
  t < t0 ? 0 : t < t0 + up ? (t - t0) / up : t < t0 + dur - down ? 1 : clamp(1 - (t - (t0 + dur - down)) / down);

// ---- camera: [time, x, y, scale, rotateY], one table per format ---------------------------------
const VERTICAL = document.body.classList.contains('v');
const W = VERTICAL ? 1080 : 1920;
const CAM_H = [
  [0, 1000, 60, 0.92, -32], [4.6, 1000, 60, 0.92, -32], [6.2, 0, 0, 1, -9], [9.4, 0, 0, 1, -9],
  [10.8, 0, 182, 1.55, -4], [15.2, 0, 182, 1.55, -4],
  [16.9, 37, 229, 1.25, -6], [21.8, 37, 229, 1.25, -6],
  [23.2, 0, 0, 1, -6], [31.6, 0, 0, 1, -6],
  [33.0, 0, 0, 1.02, -9], [37.8, 0, 0, 1.02, -9],
  [39.2, 45, 316, 1.5, -5], [43.8, 45, 316, 1.5, -5],
  [45.8, 1000, 0, 0.95, -30], [TOTAL, 1000, 0, 0.95, -30],
];
// Portrait: copy on top, the TV above the phone while typing and while using the keys.
const CAM_V = [
  [0, 700, 100, 0.95, -32], [4.6, 700, 100, 0.95, -32], [6.2, 0, 0, 1.25, -9], [9.4, 0, 0, 1.25, -9],
  [10.8, 0, 145, 1.7, -4], [15.2, 0, 145, 1.7, -4],
  [16.9, 45, 157, 1.5, -6], [21.8, 45, 157, 1.5, -6],
  [23.2, 0, 299, 1.05, -6], [31.6, 0, 299, 1.05, -6],
  [33.0, 0, 299, 1.05, -9], [37.8, 0, 299, 1.05, -9],
  [39.2, 51, 267, 1.7, -5], [43.8, 51, 267, 1.7, -5],
  [45.8, 900, 0, 1, -30], [TOTAL, 900, 0, 1, -30],
];
const CAM = VERTICAL ? CAM_V : CAM_H;
function camera(t) {
  for (let i = 1; i < CAM.length; i++) {
    if (t <= CAM[i][0]) {
      const a = CAM[i - 1], b = CAM[i], k = E.io3(prog(t, a[0], b[0]));
      return [1, 2, 3, 4].map((j) => lerp(a[j], b[j], k));
    }
  }
  return CAM[CAM.length - 1].slice(1);
}

// ---- taps: where the finger lands, in phone-screen coordinates ----------------------------
const RING = [195, 342], R = 121;
const TAPS = {
  up: [RING[0], RING[1] - 0.7 * R, 11.6, 0.55], right: [RING[0] + 0.7 * R, RING[1], 12.6, 0.55],
  down: [RING[0], RING[1] + 0.7 * R, 13.6, 0.55], left: [RING[0] - 0.7 * R, RING[1], 14.6, 0.55],
  ok: [RING[0], RING[1], 15.3, 0.5],
  bar: [150, 76, 17.9, 0.5], row2: [150, 179, 20.0, 0.5],
  kbd: [275, 149, 23.6, 0.5], valider: [322, 470, 30.2, 0.5],
  play1: [127, 669, 33.4, 0.5], play2: [127, 669, 34.4, 0.5], volup: [262, 680, 35.2, 1.0], mute: [127, 788, 36.8, 0.5],
  reco: [349, 77, 41.2, 0.5],
};
const tap = (name, t) => bump(t, TAPS[name][2], TAPS[name][3]);

const WORD = 'interstellar', STEP = 0.36, TYPE0 = 25.6;
const typedCount = (t) => (t < TYPE0 ? 0 : Math.min(WORD.length, Math.floor((t - TYPE0) / STEP) + 1));

// ---- scenes' copy slides: [index, start, end] ---------------------------------------------
const SLIDES = [[0, 5.2, 9.4], [1, 10.2, 15.6], [2, 16.6, 22.4], [3, 23.2, 31.8], [4, 32.6, 38], [5, 38.8, 44]];

function seek(t) {
  const [cx, cy, cs, cry] = camera(t);
  const sway = Math.sin(t * 0.8) * 1.4;
  el.phoneWrap.style.opacity = prog(t, 4.2, 5.4) * (1 - prog(t, 45.6, 46.8));
  el.phoneWrap.style.transform = `translate(${cx}px, ${cy}px) scale(${cs}) rotateY(${cry + sway}deg)`;

  introOutro(t);
  slides(t);
  popIn(t);
  connection(t);
  wheel(t);
  devices(t);
  typing(t);
  controls(t);
  finger(t);
  tv(t);

  el.brand.style.opacity = prog(t, 4.8, 5.6) * (1 - prog(t, 43.8, 44.4));
  el.bar.style.width = `${(t / TOTAL) * W}px`;
  el.bar.style.opacity = prog(t, 4.4, 5) * (1 - prog(t, 44, 44.6));
}

function introOutro(t) {
  const i = el.intro;
  i.style.opacity = prog(t, 0.1, 0.5) * (1 - prog(t, 3.6, 4.3));
  i.style.transform = `translateY(${-60 * E.in2(prog(t, 3.6, 4.3))}px)`;
  const icon = i.querySelector('.appicon');
  icon.style.transform = `scale(${lerp(0.5, 1, E.back(prog(t, 0.2, 1.2)))})`;
  $('introTitle').style.opacity = prog(t, 1.0, 1.7);
  $('introTitle').style.transform = `translateY(${lerp(40, 0, E.out3(prog(t, 1.0, 1.9)))}px)`;
  $('introTag').style.opacity = prog(t, 1.9, 2.7);

  const o = el.outro;
  o.style.opacity = prog(t, 45.2, 46.2);
  const kids = [...o.children];
  kids.forEach((k, n) => {
    const p = E.out3(prog(t, 45.4 + n * 0.18, 46.4 + n * 0.18));
    k.style.opacity = p;
    k.style.transform = `translateY(${(1 - p) * 40}px)`;
  });
}

function slides(t) {
  el.slides.forEach((s, i) => {
    const [, a, b] = SLIDES[i];
    const out = E.in2(prog(t, b - 0.6, b));
    const live = t >= a - 0.01 && t <= b;
    s.root.style.opacity = live ? 1 - out : 0;
    s.root.style.transform = `translateY(${-26 * out}px)`;
    s.eyebrow.style.opacity = prog(t, a, a + 0.5);
    s.words.forEach((w, n) => {
      const p = E.out3(prog(t, a + 0.15 + n * 0.07, a + 0.85 + n * 0.07));
      w.style.transform = `translateY(${(1 - p) * 115}%)`;
    });
    const sp = prog(t, a + 0.7, a + 1.3);
    s.sub.style.opacity = sp;
    s.sub.style.transform = `translateY(${(1 - sp) * 18}px)`;
  });
}

// The parts of the remote appear one after the other when the phone arrives.
function popIn(t) {
  const order = ['win', 'reco', 'power', 'kbd', 'ring', 'back', 'home', 'play', 'mute', 'vol'];
  order.forEach((id, n) => {
    const p = prog(t, 5.0 + n * 0.12, 5.8 + n * 0.12);
    const e = el[id];
    e.style.setProperty('--op', E.out3(p));
    e.style.setProperty('--sc', lerp(0.7, 1, E.back(p)));
    if (id === 'win' || id === 'reco') {
      e.style.opacity = E.out3(p);
      e.style.translate = `0 ${(1 - E.out3(p)) * -14}px`;
    }
  });
}

function connection(t) {
  const lost = t >= 39.4 && t < 41.2, connecting = t >= 41.2 && t < 42.0;
  const dot = t >= 20.2 && t < 20.8 ? 'var(--muted)' : lost ? 'var(--offline)' : connecting ? 'var(--muted)' : 'var(--online)';
  el.statusDot.style.background = dot;
  const dim = lost || connecting ? 0.35 : 1;
  ['power', 'kbd', 'back', 'home', 'play', 'mute', 'vol'].forEach((id) => el[id].style.setProperty('--gl', dim));
  el.recoSvg.style.stroke = lost ? 'var(--amber)' : 'var(--muted)';
  el.recoSvg.style.opacity = lost ? 1 : 0.6;
  el.recoSvg.style.transform = `rotate(${360 * E.io3(prog(t, 41.3, 42.0))}deg)`;
  const phase = (t * 1.1) % 1;
  el.recoPulse.style.opacity = lost ? 0.8 * (1 - phase) : 0;
  el.recoPulse.style.transform = `scale(${1 + phase * 0.45})`;
  el.reco.style.transform = `scale(${1 - 0.1 * tap('reco', t)})`;
}

function wheel(t) {
  const dirs = { up: -45, right: 45, down: 135, left: 225 };
  let active = null;
  for (const d of Object.keys(dirs)) if (tap(d, t) > 0) active = d;
  const ok = tap('ok', t);
  el.sector.style.opacity = active ? tap(active, t) : 0;
  if (active) el.sector.style.background = `conic-gradient(from ${dirs[active]}deg, rgba(0,0,0,.34) 0 90deg, transparent 90deg)`;
  for (const [d, led] of Object.entries(el.leds)) led.classList.toggle('lit', d === active && tap(d, t) > 0.3);
  el.knob.style.setProperty('--kp', 1 - 0.03 * ok);
  el.knob.style.setProperty('--kd', 0.22 * ok);
}

function devices(t) {
  const open = E.out3(prog(t, 18.0, 18.6)) - E.out3(prog(t, 20.65, 21.15));
  el.menu.style.opacity = open;
  el.menu.style.transform = `scaleY(${0.9 + 0.1 * open}) translateY(${(1 - open) * -8}px)`;
  const swapped = t >= 20.2;
  $('chk1').style.opacity = swapped ? 0 : 1;
  $('chk2').style.opacity = swapped ? 1 : 0;
  $('winName').textContent = swapped ? 'Chambre' : 'Family Room TV';
  $('winName').style.opacity = 1 - 0.8 * bump(t, 20.15, 0.3, 0.1, 0.1);
  el.win.style.transform = `scale(${1 - 0.025 * tap('bar', t)})`;
}

function typing(t) {
  const shown = E.out3(prog(t, 24.0, 24.8)) - E.out3(prog(t, 30.9, 31.5));
  const n = typedCount(t);
  const text = WORD.slice(0, n);
  el.scrim.style.opacity = 0.55 * shown;
  el.scrim.style.visibility = shown > 0.001 ? 'visible' : 'hidden';
  el.sheet.style.opacity = shown;
  el.sheet.style.transform = `translateY(${(1 - shown) * 60}px)`;
  el.kb.style.transform = `translateY(${(1 - shown) * 316}px)`;
  el.typed.textContent = text;
  el.ph.style.display = n > 0 ? 'none' : '';
  el.caret.style.opacity = n > 0 || Math.floor(t * 1.8) % 2 === 0 ? 1 : 0;
  el.ok.style.setProperty('--sc', 1 - 0.07 * tap('valider', t));
  Object.entries(el.keys).forEach(([c, k]) => k.classList.remove('on'));
  for (let i = 0; i < n; i++) {
    if (t - (TYPE0 + i * STEP) < 0.22) el.keys[WORD[i]].classList.add('on');
  }
  el.kbd.style.setProperty('--p', tap('kbd', t));
}

const PAUSE = '<svg viewBox="0 0 24 24"><path d="M8 5v14M16 5v14" stroke-linecap="round" fill="none"/></svg>';
const PLAY = '<svg viewBox="0 0 24 24"><path d="M8 5l11 7-11 7Z" stroke-linejoin="round"/></svg>';
const SPEAKER = '<path d="M3 9.5h4.5L13 5v14l-5.5-4.5H3Z"/><path d="M16.5 9a4.5 4.5 0 0 1 0 6"/>';

function controls(t) {
  ['back', 'home', 'play', 'mute'].forEach((id) => el[id].style.setProperty('--p', 0));
  el.play.style.setProperty('--p', Math.max(tap('play1', t), tap('play2', t)));
  el.mute.style.setProperty('--p', tap('mute', t));
  el.vol.style.setProperty('--p', tap('volup', t));
  el.power.style.setProperty('--p', 0);
  el.back.style.setProperty('--p', 0);
  el.home.style.setProperty('--p', 0);
}

function finger(t) {
  let best = null;
  for (const [name, [x, y, t0, dur]] of Object.entries(TAPS)) {
    if (t >= t0 - 0.25 && t <= t0 + dur + 0.25) best = { x, y, t0, dur, name };
  }
  const f = el.finger;
  if (!best) { f.style.opacity = 0; return; }
  const a = prog(t, best.t0 - 0.25, best.t0) * (1 - prog(t, best.t0 + best.dur, best.t0 + best.dur + 0.25));
  const press = bump(t, best.t0, best.dur);
  f.style.left = `${best.x}px`;
  f.style.top = `${best.y}px`;
  f.style.opacity = a;
  f.style.transform = `scale(${1.3 - 0.4 * press})`;
}

function tv(t) {
  el.tv.style.opacity = E.out3(prog(t, 23.0, 24.0)) * (1 - E.in2(prog(t, 38.0, 38.8)));
  el.tv.style.transform = `translateY(${(1 - E.out3(prog(t, 23.0, 24.2))) * 60}px)`;
  const play = E.io3(prog(t, 31.8, 32.6));
  el.tvSearch.style.opacity = 1 - play;
  el.tvPlay.style.opacity = play;

  const n = typedCount(t);
  el.tvTyped.textContent = WORD.slice(0, n);
  el.tvPh.style.display = n > 0 ? 'none' : '';
  [prog(n, 0, 2), prog(n, 4, 6), prog(n, 6, 8)].forEach((v, i) => (el.sg[i].style.opacity = v));
  el.cards.forEach((c, i) => {
    const hit = t >= 30.2 && i === 0;
    c.style.opacity = prog(n, 8 + i * 0.7, 9.5 + i * 0.7);
    c.style.transform = `scale(${hit ? 1.06 : 1})`;
    c.style.outline = hit ? '3px solid #fff' : 'none';
  });
  el.sg[0].classList.toggle('hit', t >= 30.2);

  // Playing screen: pause/play flash, then the volume HUD.
  const g1 = bump(t, 33.45, 0.9, 0.12, 0.6), g2 = bump(t, 34.45, 0.9, 0.12, 0.6);
  el.glyph.innerHTML = g1 > 0 ? PAUSE : PLAY;
  el.glyph.style.opacity = Math.max(g1, g2);
  el.glyph.style.transform = `scale(${lerp(0.85, 1.1, Math.max(g1, g2))})`;
  const hud = bump(t, 35.1, 2.5, 0.2, 0.4);
  el.hud.style.opacity = hud;
  const muted = t >= 36.8;
  el.hudIcon.innerHTML = muted ? ICON.mute : SPEAKER;
  const level = muted ? 0 : 40 + 35 * E.io3(prog(t, 35.2, 36.2));
  el.hudFill.style.width = `${level}%`;
}

window.seek = seek;
seek(0);
