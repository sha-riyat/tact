// Builds the phone, the TV and the copy blocks once; scenes.js then drives them frame by frame.
if (new URLSearchParams(location.search).has('v')) {
  document.documentElement.classList.add('v');
  document.body.classList.add('v');
}
const $ = (id) => document.getElementById(id);
const make = (html) => { const t = document.createElement('template'); t.innerHTML = html.trim(); return t.content.firstChild; };

// Same glyphs as the app, on the same 24-unit grid.
const ICON = {
  power: '<path d="M15.75 6.505A7.5 7.5 0 1 1 8.25 6.505"/><path d="M12 3v9"/>',
  keyboard: '<rect x="2" y="4" width="20" height="16" rx="2.5"/><path d="M6 9h12M6 13h.1M10 13h4M18 13h.1M8 16.5h8"/>',
  back: '<path d="M15 4 7 12l8 8"/>',
  home: '<path d="M3.5 11 12 3.5 20.5 11v9.5h-6V14h-5v6.5h-6Z"/>',
  play: '<path d="M3 5l10 7-10 7Z"/><path d="M17 5v14M21 5v14"/>',
  mute: '<path d="M3 9.5h4.5L13 5v14l-5.5-4.5H3Z"/><path d="M17 9l5 6M22 9l-5 6"/>',
  plus: '<path d="M12 4v16M4 12h16"/>',
  minus: '<path d="M4 12h16"/>',
  chevron: '<path d="M5 9l7 7 7-7"/>',
  check: '<path d="M4 12.5l5.5 5.5L20 6.5"/>',
  refresh: '<path d="M19.5 12A7.5 7.5 0 1 1 15.75 5.505"/><path d="M12.4 7.3l5.1-.8-1.9-4.8"/>',
};
const svg = (name, extra = '') =>
  `<svg viewBox="0 0 24 24" fill="none" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" ${extra}>${ICON[name]}</svg>`;

const TONES = {
  dark: ['var(--cap)', 'var(--cap-lift)', '#d9dcde'],
  paper: ['var(--paper)', 'var(--paper-lift)', '#2a2e31'],
  amber: ['var(--amber)', 'var(--amber-lift)', '#2b2007'],
};

// One 128 px tile of faint noise, drawn once and reused as the grain on the stage and on the phone.
(() => {
  const c = document.createElement('canvas');
  c.width = c.height = 128;
  const g = c.getContext('2d'), img = g.createImageData(128, 128);
  let seed = 7;
  const rnd = () => ((seed = (seed * 16807) % 2147483647) / 2147483647);
  for (let i = 0; i < img.data.length; i += 4) {
    const v = 120 + rnd() * 135;
    img.data[i] = img.data[i + 1] = img.data[i + 2] = v;
    img.data[i + 3] = rnd() * 20;
  }
  g.putImageData(img, 0, 0);
  document.documentElement.style.setProperty('--grain', `url(${c.toDataURL()})`);
})();

const screen = $('screen');
const el = {};

screen.insertAdjacentHTML('beforeend', `<div id="status"><span>13:07</span><span><i></i><i></i><i></i></span></div>`);

function addCap(id, x, y, w, h, tone, glyph) {
  const [base, lift, ink] = TONES[tone];
  const e = make(`<div class="cap ${w === h ? '' : 'pill'}" style="left:${x}px;top:${y}px;width:${w}px;height:${h}px;--base:${base};--lift:${lift};--glyph:${ink}"></div>`);
  if (glyph) e.insertAdjacentHTML('beforeend', svg(glyph));
  screen.appendChild(e);
  el[id] = e;
  return e;
}

// Top strip: the TV name window and the reconnect key.
screen.insertAdjacentHTML('beforeend', `
  <div id="win"><span class="dot"></span><span id="winName">Family Room TV</span>${svg('chevron')}</div>
  <div id="reco"><span class="pulse"></span>${svg('refresh')}</div>
  <div id="menu">
    <div class="row"><span id="m1">Family Room TV</span><span id="chk1">${svg('check')}</span></div>
    <div class="row"><span id="m2">Chambre</span><span id="chk2" style="opacity:0">${svg('check')}</span></div>
    <div class="row muted"><span>+&nbsp;&nbsp;Ajouter un appareil</span></div>
  </div>`);
el.win = $('win'); el.reco = $('reco'); el.menu = $('menu');

addCap('power', 113, 149, 68, 68, 'amber', 'power');
addCap('kbd', 275, 149, 68, 68, 'dark', 'keyboard');

screen.insertAdjacentHTML('beforeend', `
  <div id="ring" style="left:195px;top:342px;width:242px;height:242px">
    <div class="dish"></div>
    <div class="sector"></div>
    <div class="knob"></div>
    <div class="led" data-d="up" style="left:50%;top:21%"></div>
    <div class="led" data-d="right" style="left:79%;top:50%"></div>
    <div class="led" data-d="down" style="left:50%;top:79%"></div>
    <div class="led" data-d="left" style="left:21%;top:50%"></div>
  </div>`);
el.ring = $('ring');
el.sector = el.ring.querySelector('.sector');
el.knob = el.ring.querySelector('.knob');
el.leds = Object.fromEntries([...el.ring.querySelectorAll('.led')].map((l) => [l.dataset.d, l]));

addCap('back', 127, 549, 95, 95, 'dark', 'back');
addCap('home', 262, 549, 95, 95, 'paper', 'home');
addCap('play', 127, 669, 95, 95, 'dark', 'play');
addCap('mute', 127, 788, 95, 95, 'dark', 'mute');
const vol = addCap('vol', 262, 721, 95, 189, 'dark', null);
vol.insertAdjacentHTML('beforeend', `<div class="half up">${svg('plus')}</div><div class="half dn">${svg('minus')}</div><div class="seam"></div>`);

// Fake on-screen keyboard (AZERTY) so typing looks real.
const rows = ['1234567890', 'azertyuiop', 'qsdfghjklm', 'wxcvbn'];
screen.insertAdjacentHTML('beforeend', `<div id="scrim"></div>
  <div id="sheet">
    <div class="lab">CLAVIER</div>
    <div class="field"><span class="ph">Texte à envoyer</span><span id="typed"></span><span class="caret"></span></div>
    <div class="foot"><span>Fermer</span><div class="ok" id="ok">Valider</div></div>
  </div>
  <div id="kb"><div class="r sug"><span>■■</span><span>GIF</span><span>⎘</span><span>🎤</span></div>
    ${rows.map((r) => `<div class="r">${[...r].map((c) => `<div class="k" data-c="${c}">${c}</div>`).join('')}</div>`).join('')}
    <div class="r"><div class="k" style="max-width:90px">?123</div><div class="k" style="flex:5;max-width:none">FR · EN</div><div class="k" style="max-width:90px">➤</div></div>
  </div>`);
el.scrim = $('scrim'); el.sheet = $('sheet'); el.kb = $('kb'); el.ok = $('ok');
el.typed = $('typed'); el.ph = el.sheet.querySelector('.ph'); el.caret = el.sheet.querySelector('.caret');
el.keys = Object.fromEntries([...el.kb.querySelectorAll('.k[data-c]')].map((k) => [k.dataset.c, k]));

screen.insertAdjacentHTML('beforeend', `<div class="finger" id="finger"></div>`);
el.finger = $('finger');

// TV mock: a search screen and a "playing" screen.
$('tv').innerHTML = `<div id="tvScreen">
  <div class="layer" id="tvSearch">
    <div class="pill"><svg viewBox="0 0 24 24"><circle cx="10.5" cy="10.5" r="6.5"/><path d="M15.5 15.5 21 21"/></svg><span class="ph">Rechercher</span><span id="tvTyped"></span><span class="tcaret"></span></div>
    <div class="sg"><div class="first">interstellar</div><div>interstellar bande-annonce</div><div>interstellar musique</div></div>
    <div class="cards"><div class="card"></div><div class="card"></div><div class="card"></div><div class="card"></div></div>
  </div>
  <div class="layer" id="tvPlay"><div class="stars"></div><div class="stars b"></div>
    <div class="glyph" id="tvGlyph"></div>
    <div id="hud"><svg viewBox="0 0 24 24" id="hudIcon">${ICON.mute}</svg><div class="track"><div class="fill" id="hudFill"></div></div></div>
  </div></div>`;
el.tv = $('tv'); el.tvSearch = $('tvSearch'); el.tvPlay = $('tvPlay');
el.tvTyped = $('tvTyped'); el.tvPh = el.tvSearch.querySelector('.ph');
el.sg = [...el.tvSearch.querySelectorAll('.sg div')]; el.cards = [...el.tvSearch.querySelectorAll('.card')];
el.glyph = $('tvGlyph'); el.hud = $('hud'); el.hudFill = $('hudFill'); el.hudIcon = $('hudIcon');
el.statusDot = el.win.querySelector('.dot');
el.recoSvg = el.reco.querySelector('svg'); el.recoPulse = el.reco.querySelector('.pulse');

// Copy blocks: one slide per scene, each word masked so it can slide up.
const COPY = [
  ['UNE SEULE SURFACE', 'Tout ce qu’il faut. Rien de plus.', 'Alimentation, molette, retour, accueil, lecture et volume.'],
  ['NAVIGATION', 'Une molette. Quatre LED.', 'Chaque appui s’allume. Maintiens le doigt pour répéter.'],
  ['APPAREILS', 'Tous tes téléviseurs, un geste.', 'Appairage par code, une seule fois. Ensuite, un appui sur le nom.'],
  ['SAISIE', 'Tu tapes. La TV écrit.', 'Lettre par lettre, en direct. Fini le clavier à l’écran.'],
  ['ESSENTIELS', 'Au bout du pouce.', 'Volume, lecture, muet, accueil, retour. Rien d’autre.'],
  ['CONNEXION', 'Coupure ? Un appui.', 'Le bouton ambre te remet en ligne.'],
];
const words = (s) => s.split(' ').map((w) => `<span class="word"><span>${w}</span></span>`).join(' ');
el.slides = COPY.map(([e, h, p]) => {
  const s = make(`<div class="slide"><div class="eyebrow">${e}</div><div class="h1">${words(h)}</div><div class="sub">${p}</div></div>`);
  $('copy').appendChild(s);
  return { root: s, eyebrow: s.querySelector('.eyebrow'), words: [...s.querySelectorAll('.word > span')], sub: s.querySelector('.sub') };
});
el.phoneWrap = $('phoneWrap'); el.brand = $('brand'); el.bar = $('bar');
el.intro = $('intro'); el.outro = $('outro');
