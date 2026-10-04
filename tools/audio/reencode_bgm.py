"""Re-encodes the bundled background music at a lower bitrate to shrink the app (plan 04, D3).

Decodes each MP3 with miniaudio and re-encodes it with LAME (lameenc) at BITRATE kbps, joint
stereo, 44.1 kHz, highest-quality encoder setting. Originals are kept in tools/audio/originals/
(not packaged) so this can be re-run from the source files.

    pip install miniaudio lameenc
    python tools/audio/reencode_bgm.py [bitrate] [file ...]
"""
import os
import shutil
import sys

import lameenc
import miniaudio

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.normpath(os.path.join(HERE, "..", "..", "app", "src", "main", "res", "raw"))
ORIGINALS = os.path.join(HERE, "originals")
BITRATE = int(sys.argv[1]) if len(sys.argv) > 1 else 96
FILES = sys.argv[2:] or ["bgm_sunny.mp3", "bgm_rain.mp3", "bgm_snow.mp3", "bgm_sakura.mp3", "bgm_autumn.mp3"]


def reencode(name):
    os.makedirs(ORIGINALS, exist_ok=True)
    original = os.path.join(ORIGINALS, name)
    if not os.path.exists(original):
        shutil.copy2(os.path.join(RAW, name), original)
    decoded = miniaudio.decode_file(original, output_format=miniaudio.SampleFormat.SIGNED16, nchannels=2, sample_rate=44100)
    enc = lameenc.Encoder()
    enc.set_bit_rate(BITRATE)
    enc.set_in_sample_rate(44100)
    enc.set_channels(2)
    enc.set_quality(2)  # 2 = high quality, slower
    data = enc.encode(decoded.samples.tobytes()) + enc.flush()
    open(os.path.join(RAW, name), "wb").write(data)
    print(f"{name}: {os.path.getsize(original) / 1e6:.2f} MB -> {len(data) / 1e6:.2f} MB at {BITRATE} kbps")


if __name__ == "__main__":
    for f in FILES:
        reencode(f)
