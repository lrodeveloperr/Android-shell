#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
manifest="$root/app/src/main/AndroidManifest.xml"
ads_manifest="$root/app/src/ads/AndroidManifest.xml"
config="$root/app/src/main/java/com/goodusestudios/shell/ui/ShellConfig.kt"
localization="$root/app/src/main/assets/gooduse-common-localization-v1.json"

required=(
  "$root/branding/play-store-icon.svg"
  "$root/branding/play-store-icon-512.png"
  "$root/app/src/main/res/drawable/ic_brand_mark.xml"
  "$root/app/src/main/res/drawable/ic_launcher_foreground.xml"
  "$root/app/src/main/res/drawable/ic_launcher_monochrome.xml"
  "$root/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"
  "$root/app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml"
  "$root/app/src/main/java/com/goodusestudios/shell/data/AccessPolicy.kt"
  "$root/app/src/main/java/com/goodusestudios/shell/data/BackupProvider.kt"
  "$root/app/src/main/java/com/goodusestudios/shell/ui/FeatureCanvas.kt"
  "$root/app/src/androidTest/java/com/goodusestudios/shell/PaywallComplianceUiTest.kt"
  "$root/AGENTS.md"
  "$root/docs/STORE_COMPLIANCE_CHECKLIST.md"
  "$root/docs/APP_POLICY_PROFILE.md"
  "$root/docs/UI_REGRESSION_MATRIX.md"
  "$root/docs/SHELL_MIGRATIONS.md"
  "$root/app/src/main/res/xml/locales_config.xml"
  "$ads_manifest"
  "$root/app/src/noAds/AndroidManifest.xml"
  "$localization"
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

python3 - "$localization" "$root/app/src/main/res/xml/locales_config.xml" <<'PY'
import hashlib, json, pathlib, re, sys
bundle_path, config_path = map(pathlib.Path, sys.argv[1:])
raw = bundle_path.read_bytes().rstrip(b'\n')
bundle = json.loads(raw)
expected = ['en','es','pt','fr','de','it','nl','pl','tr','ro','cs','uk','ru','ar','zh','ja','ko','hi','ur','bn','vi','id','th','fil','ms','fi','sv','da','nb','el','he']
if bundle.get('contractVersion') != '1.0.0' or bundle.get('locales') != expected:
    raise SystemExit('Locked localization contract or locale order changed')
if hashlib.sha256(raw).hexdigest() != 'e2179589f18d999774eb2c36b3453a55520495783991995b7d38891f76c686d9':
    raise SystemExit('Locked localization bundle hash changed')
configured = re.findall(r'android:name="([^"]+)"', config_path.read_text())
if configured != expected:
    raise SystemExit('Android selectable locales must match the locked 31-locale bundle')
if any(not key.startswith('common.') for key in bundle.get('entries', {})):
    raise SystemExit('Localization bundle contains a non-common semantic key')
PY

if grep -R -n -E --include='*.kt' --include='*.kts' --include='*.xml' '(io\.flutter|FlutterActivity|flutter:)' "$root/app" "$root/build.gradle.kts" "$root/settings.gradle.kts" >/dev/null; then
  echo "Flutter code or dependencies are forbidden in the native Jetpack shell" >&2
  exit 1
fi

if grep -R -n -E 'PurchaseVerifier[[:space:]]*\{[[:space:]]*true[[:space:]]*\}' "$root/app/src/main/java" >/dev/null; then
  echo "Fail-open purchase verifier detected" >&2
  exit 1
fi

grep -q 'reportSuccessfulAction' "$root/app/src/main/java/com/goodusestudios/shell/ui/FeatureCanvas.kt"
grep -q 'recordSuccessfulAction' "$root/app/src/main/java/com/goodusestudios/shell/data/ShellStateStore.kt"
grep -q 'purchase.products.all' "$root/app/src/main/java/com/goodusestudios/shell/data/BillingController.kt"

if [[ "${1:-}" == "--strict" ]]; then
  release_flavor="${2:-ads}"
  [[ "$release_flavor" == "ads" || "$release_flavor" == "noAds" ]] || {
    echo "Strict flavor must be ads or noAds" >&2
    exit 1
  }
  grep -q 'appName = "Shell"' "$config" && { echo "Replace template app name" >&2; exit 1; }
  grep -q 'example.com' "$config" && { echo "Replace example legal/support values" >&2; exit 1; }
  grep -q 'shell.pro.' "$config" && { echo "Replace template Play product IDs" >&2; exit 1; }
  if grep -q -E 'initialMode = MonetizationMode\.(AdsWithRemovePurchase|OneTimeUnlock|Subscription|UsageCapWithOneTimeUnlock|UsageCapWithSubscription)' "$config"; then
    grep -q 'playLicensePublicKey = ""' "$config" && { echo "Configure Play signature verification or inject a trusted PurchaseVerifier" >&2; exit 1; }
  fi
  if [[ "$release_flavor" == "ads" ]]; then
    grep -q 'ca-app-pub-3940256099942544' "$ads_manifest" && { echo "Replace Google test AdMob app ID" >&2; exit 1; }
  elif grep -q -E 'initialMode = MonetizationMode\.(Ads|AdsWithRemovePurchase)' "$config"; then
    echo "The noAds release flavor cannot use an ads monetization profile" >&2
    exit 1
  fi
fi

# Locked contract and conservative defaults.
grep -q 'const val contractVersion = "2.1.0"' "$config"
grep -q 'presentation = OnboardingPresentation.None' "$config"
grep -q 'object DisabledBackupProvider' "$root/app/src/main/java/com/goodusestudios/shell/data/BackupProvider.kt"

# Purchase surfaces must never contain the app logo, launcher icon, or any brand-mark image.
paywall_source="$(sed -n '/^fun PaywallScreen(/,/^fun LegalScreen(/p' "$root/app/src/main/java/com/goodusestudios/shell/ui/ShellScreens.kt")"
for forbidden in 'Image(' 'painterResource(' 'ic_brand_mark' 'ic_launcher'; do
  if grep -Fq "$forbidden" <<<"$paywall_source"; then
    echo "Purchase-surface policy violation: PaywallScreen contains $forbidden" >&2
    exit 1
  fi
done
for required_text in 'Restore purchases' 'Privacy' 'Terms' 'renews automatically' 'One-time purchase'; do
  grep -Fq "$required_text" <<<"$paywall_source" || {
    echo "Purchase-surface disclosure missing: $required_text" >&2
    exit 1
  }
done

if [[ "${1:-}" == "--strict" ]] && grep -q 'DECISION_REQUIRED' "$root/docs/APP_POLICY_PROFILE.md"; then
  echo "Strict release blocked: complete docs/APP_POLICY_PROFILE.md." >&2
  exit 1
fi

echo "Shell structure validated${1:+ ($1)}."
