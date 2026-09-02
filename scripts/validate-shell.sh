#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
manifest="$root/app/src/main/AndroidManifest.xml"
config="$root/app/src/main/java/com/goodusestudios/shell/ui/ShellConfig.kt"

required=(
  "$root/branding/play-store-icon.svg"
  "$root/branding/play-store-icon-512.png"
  "$root/app/src/main/res/drawable/ic_brand_mark.xml"
  "$root/app/src/main/res/drawable/ic_launcher_foreground.xml"
  "$root/app/src/main/res/drawable/ic_launcher_monochrome.xml"
  "$root/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"
  "$root/app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml"
)
for file in "${required[@]}"; do
  [[ -s "$file" ]] || { echo "Missing required shell asset: $file" >&2; exit 1; }
done

grep -q 'android:icon="@mipmap/ic_launcher"' "$manifest"
grep -q 'android:roundIcon="@mipmap/ic_launcher_round"' "$manifest"
grep -q 'android:theme="@style/Theme.Shell.Starting"' "$manifest"
grep -q '<monochrome android:drawable="@drawable/ic_launcher_monochrome"' "$root/app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml"
grep -q 'windowSplashScreenAnimatedIcon' "$root/app/src/main/res/values/styles.xml"

python3 - "$root/branding/play-store-icon-512.png" <<'PY'
import struct, sys
with open(sys.argv[1], 'rb') as image:
    header = image.read(24)
if header[:8] != b'\x89PNG\r\n\x1a\n':
    raise SystemExit('Play Store icon is not a PNG')
width, height = struct.unpack('>II', header[16:24])
if (width, height) != (512, 512):
    raise SystemExit(f'Play Store icon must be 512x512, got {width}x{height}')
PY

if [[ "${1:-}" == "--strict" ]]; then
  grep -q 'appName = "Shell"' "$config" && { echo "Replace template app name" >&2; exit 1; }
  grep -q 'example.com' "$config" && { echo "Replace example legal/support values" >&2; exit 1; }
  grep -q 'shell.pro.' "$config" && { echo "Replace template Play product IDs" >&2; exit 1; }
  grep -q 'ca-app-pub-3940256099942544' "$manifest" && { echo "Replace Google test AdMob app ID" >&2; exit 1; }
fi

echo "Shell structure validated${1:+ ($1)}."
