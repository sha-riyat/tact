"""Sound design for the Tact demo, synthesised with numpy only (no samples, no downloads).

Every sound is placed at the time of the matching gesture in scenes.js, so the soundtrack stays in step with
the picture. Run:  python -I sound.py  ->  audio/tact-demo.wav  (48 kHz stereo, 50 s)
"""
import math
import os
import wave

import numpy as np

SR = 48000
DURATION = 50.0
N = int(SR * DURATION)
rng = np.random.default_rng(7)
mix = np.zeros((N, 2))


def add(sig, t, gain=1.0, pan=0.0):
    """Mixes a mono signal in at time t (seconds); pan runs from -1 (left) to 1 (right)."""
    i = int(t * SR)
    if i >= N:
        return
    sig = sig[: N - i]
    left, right = math.cos((pan + 1) * math.pi / 4), math.sin((pan + 1) * math.pi / 4)
    mix[i:i + len(sig), 0] += sig * gain * left * math.sqrt(2)
    mix[i:i + len(sig), 1] += sig * gain * right * math.sqrt(2)


def tvec(dur):
    return np.arange(int(SR * dur)) / SR


def decay(dur, rate):
    return np.exp(-rate * tvec(dur))


def lowpass(x, cutoff):
    a = 1 - math.exp(-2 * math.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += a * (v - acc)
        y[i] = acc
    return y


def sweep_noise(dur, f0, f1, q=2.5):
    """Band-passed noise whose centre frequency glides from f0 to f1 (a state-variable filter)."""
    n = int(SR * dur)
    freqs = np.geomspace(f0, f1, n)
    x = rng.standard_normal(n)
    out = np.empty(n)
    low = band = 0.0
    for i in range(n):
        f = 2 * math.sin(math.pi * freqs[i] / SR)
        low += f * band
        high = x[i] - low - band / q
        band += f * high
        out[i] = band
    return out / (np.max(np.abs(out)) + 1e-9)


def click(pitch=1.0, amp=1.0):
    """A soft key click: a short breath of noise on top of a small wooden thump."""
    d = 0.09
    thump = np.sin(2 * math.pi * 170 * pitch * tvec(d)) * decay(d, 70)
    air = lowpass(rng.standard_normal(int(SR * d)), 5200) * decay(d, 160) * 0.55
    return (thump * 0.8 + air) * amp


def pluck(freq, dur=0.7, amp=1.0):
    """Marimba-like note: a sine with a quick-fading upper partial."""
    t = tvec(dur)
    s = np.sin(2 * math.pi * freq * t) * np.exp(-5 * t) + 0.35 * np.sin(2 * math.pi * freq * 4 * t) * np.exp(-18 * t)
    s *= np.minimum(1, t * 800)
    return s * amp


def chime(freq, dur=1.6, amp=1.0):
    """Glassy bell: inharmonic partials with a slow decay."""
    t = tvec(dur)
    s = sum(a * np.sin(2 * math.pi * freq * r * t) * np.exp(-k * t)
            for r, a, k in ((1, 1.0, 2.6), (2.76, 0.35, 4.5), (5.4, 0.16, 8.0)))
    return s * np.minimum(1, t * 600) * amp


def whoosh(dur, f0, f1, amp=1.0):
    n = int(SR * dur)
    env = np.sin(np.linspace(0, math.pi, n)) ** 2
    return sweep_noise(dur, f0, f1) * env * amp


def swell(dur, base, amp=1.0):
    """A rising pad note for titles: two detuned sines under a slow attack."""
    t = tvec(dur)
    env = np.minimum(1, t / (dur * 0.7)) ** 2 * np.exp(-0.9 * np.maximum(0, t - dur * 0.7))
    return (np.sin(2 * math.pi * base * t) + 0.6 * np.sin(2 * math.pi * base * 1.5 * t * 1.002)) * env * amp


# ---- scene sounds -----------------------------------------------------------------------------------------------
def intro_and_outro():
    add(swell(2.0, 110, 0.5), 0.2, pan=-0.2)
    add(chime(880, 2.2, 0.35), 1.05)
    add(chime(1318.5, 2.6, 0.22), 1.95, pan=0.2)
    add(whoosh(1.0, 400, 3200, 0.55), 3.5)

    add(swell(2.6, 98, 0.55), 44.9)
    for n, f in enumerate((659.3, 880, 1174.7)):
        add(pluck(f, 1.2, 0.5), 46.45 + n * 0.18, pan=(n - 1) * 0.4)
    add(chime(1318.5, 3.4, 0.3), 46.2)


def reveal():
    add(whoosh(1.5, 300, 2400, 0.5), 4.6)
    scale = (523.3, 587.3, 659.3, 784, 880, 1046.5, 1174.7, 1318.5, 1568, 1760)
    for n, f in enumerate(scale):
        add(pluck(f, 0.5, 0.42), 5.0 + n * 0.12 + 0.12, pan=-0.5 + n * 0.11)


def wheel():
    for t, pitch, pan in ((11.6, 1.3, 0), (12.6, 1.1, 0.4), (13.6, 0.85, 0), (14.6, 1.0, -0.4)):
        add(click(pitch, 0.9), t, pan=pan)
        add(chime(1568 * pitch, 0.5, 0.18), t + 0.02, pan=pan)
    add(click(0.7, 1.1), 15.3)
    add(chime(784, 0.9, 0.25), 15.32)


def devices():
    add(click(1.0, 0.9), 17.9)
    add(whoosh(0.5, 500, 2600, 0.35), 18.0)
    add(click(1.0, 0.9), 20.0)
    add(pluck(784, 0.6, 0.45), 20.22)
    add(pluck(1174.7, 0.8, 0.45), 20.36)
    add(whoosh(0.5, 2600, 500, 0.3), 20.65)


def typing():
    add(click(1.0, 0.9), 23.6)
    add(whoosh(0.7, 400, 2800, 0.45), 23.95)
    for i in range(12):
        t = 25.6 + i * 0.36
        add(click(0.95 + 0.12 * rng.random(), 0.8), t, pan=-0.15 + 0.3 * rng.random())
        add(click(1.5, 0.22), t + 0.0, pan=0.6)  # the matching tick on the TV
    add(click(1.0, 0.9), 30.2)
    add(pluck(880, 0.6, 0.4), 30.34)
    add(pluck(1318.5, 0.9, 0.4), 30.46)
    add(whoosh(0.6, 2800, 400, 0.4), 30.9)


def controls():
    add(click(0.9, 0.95), 33.4)
    add(pluck(523.3, 0.5, 0.35), 33.46)
    add(click(1.0, 0.95), 34.4)
    add(pluck(784, 0.5, 0.35), 34.46)
    add(click(1.0, 0.95), 35.2)
    for k in range(8):  # volume held: repeating ticks that climb
        add(click(1.0 + k * 0.05, 0.55), 35.2 + 0.45 + k * 0.1)
    add(click(0.65, 1.1), 36.8)
    add(pluck(196, 0.6, 0.45), 36.82)


def reconnect():
    add(pluck(392, 0.8, 0.45), 39.4)
    add(pluck(311.1, 1.1, 0.45), 39.58)
    add(click(1.0, 0.95), 41.2)
    add(whoosh(0.8, 500, 3600, 0.4), 41.3)
    add(chime(880, 1.4, 0.4), 42.0)
    add(chime(1108.7, 1.8, 0.35), 42.1)


def camera_moves():
    for t, lo, hi in ((10.2, 300, 1800), (16.4, 300, 1800), (22.8, 300, 1800), (32.4, 300, 1600),
                      (38.7, 300, 1800), (45.4, 300, 2200)):
        add(whoosh(1.0, lo, hi, 0.28), t)


# ---- music bed ---------------------------------------------------------------------------------------------------
CHORDS = [  # eight chords of 6.25 s: Cmaj9, Am9, Fmaj7, Gsus, then again
    (65.41, (130.81, 196.0, 246.94, 293.66, 329.63)),
    (55.0, (110.0, 164.81, 196.0, 246.94, 261.63)),
    (43.65, (130.81, 174.61, 220.0, 329.63)),
    (49.0, (146.83, 196.0, 220.0, 293.66)),
]


def music():
    seg = DURATION / 8
    for n in range(8):
        root, notes = CHORDS[n % 4]
        t0 = n * seg - 0.8 if n else 0.0
        dur = seg + 1.6 if n else seg + 0.8
        t = tvec(dur)
        env = np.minimum(1, t / 1.6) * np.minimum(1, (dur - t) / 1.6)
        pad = np.zeros(len(t))
        for f in notes:
            pad += np.sin(2 * math.pi * f * t) + 0.3 * np.sin(2 * math.pi * f * 2 * t)
        pad = pad / len(notes) * env * (0.8 + 0.2 * np.sin(2 * math.pi * 0.17 * t))
        sub = np.sin(2 * math.pi * root * t) * env
        add(pad, max(t0, 0), 0.20, pan=-0.35)
        add(pad * np.cos(2 * math.pi * 0.31 * t + 1), max(t0, 0), 0.16, pan=0.35)
        add(sub, max(t0, 0), 0.20)
    # a slow pentatonic shimmer above the pad, only while the demo runs
    notes = (1046.5, 1174.7, 1318.5, 1568, 1760)
    for k in range(int(5.0 / 1.25), int(44 / 1.25)):
        add(pluck(notes[(k * 3) % 5], 1.6, 0.05), k * 1.25, pan=((k % 4) - 1.5) * 0.4)


def master():
    global mix
    fade = np.ones(N)
    fade[: int(SR * 1.5)] = np.linspace(0, 1, int(SR * 1.5))
    fade[-int(SR * 2.2):] = np.linspace(1, 0, int(SR * 2.2))
    mix = np.tanh(mix * fade[:, None] * 0.9)
    mix /= max(1.0, np.max(np.abs(mix)) / 0.89)


if __name__ == "__main__":
    for part in (music, camera_moves, intro_and_outro, reveal, wheel, devices, typing, controls, reconnect):
        part()
    master()
    os.makedirs("audio", exist_ok=True)
    with wave.open("audio/tact-demo.wav", "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((mix * 32767).astype("<i2").tobytes())
    print(f"peak {np.max(np.abs(mix)):.2f}  rms {np.sqrt(np.mean(mix ** 2)):.3f}")
