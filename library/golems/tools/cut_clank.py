"""The golem's clank: the latch at the end of the door sound launchers give beta, a later recording under beta's name
(beta's own is what Pure Faithful's hardening fetches in its place, BetaSounds), without the swing that leads in: the door sound's second half, so no golem sound is a whole door. A
small transformative use of Mojang's recording, shipped as the mod's own file, as with Alchemy's recoloured rose.

    python library/golems/tools/cut_clank.py <door_close.ogg>

The input is the resources/newsound/random/door_close.ogg a launcher downloads with b1.7.3, from any game folder.
Writes library/golems/src/main/resources/assets/golems/sounds/clank.wav, which plays as golems.clank. Needs ffmpeg on
the path to read the Ogg.

The original is 0.39 s: a hit that dies away by 100 ms, quiet to 130 ms, then the latch, loudest at 160 ms and gone by
300 ms. The clip is 130 to 300 ms, with a 3 ms fade in and the last 20 ms faded out, so it starts and stops clean.
"""
import array
import subprocess
import sys
import wave
from pathlib import Path

RATE = 44100
START, END = 0.130, 0.300
FADE_IN, FADE_OUT = 0.003, 0.020

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/golems/sounds/clank.wav"


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", sys.argv[1], "-f", "s16le", "-ac", "1", "-ar", str(RATE), "-"],
                         check=True, capture_output=True).stdout
    pcm = array.array("h")
    pcm.frombytes(raw)
    clip = pcm[int(START * RATE):int(END * RATE)]
    fade_in, fade_out = int(FADE_IN * RATE), int(FADE_OUT * RATE)
    for i in range(len(clip)):
        gain = min(1.0, i / fade_in, (len(clip) - 1 - i) / fade_out)
        clip[i] = int(clip[i] * gain)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(clip.tobytes())
    print(f"{OUT}: {len(clip) / RATE * 1000:.0f} ms")


if __name__ == "__main__":
    main()
